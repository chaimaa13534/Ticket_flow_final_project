package com.ticketflow.bookingservice.services;

import com.ticketflow.bookingservice.clients.EventClient;
import com.ticketflow.bookingservice.clients.EventClient.EventView;
import com.ticketflow.bookingservice.clients.EventClient.TierView;
import com.ticketflow.bookingservice.dto.BookingRequest;
import com.ticketflow.bookingservice.dto.BookingRequest.LineRequest;
import com.ticketflow.bookingservice.entities.Booking;
import com.ticketflow.bookingservice.entities.BookingLine;
import com.ticketflow.bookingservice.entities.BookingStatus;
import com.ticketflow.bookingservice.entities.IdempotencyKey;
import com.ticketflow.bookingservice.entities.TierInventory;
import com.ticketflow.bookingservice.exceptions.BookingRuleException;
import com.ticketflow.bookingservice.exceptions.ResourceNotFoundException;
import com.ticketflow.bookingservice.exceptions.UpstreamUnavailableException;
import com.ticketflow.bookingservice.repositories.BookingLineRepo;
import com.ticketflow.bookingservice.repositories.BookingRepo;
import com.ticketflow.bookingservice.repositories.IdempotencyKeyRepo;
import com.ticketflow.bookingservice.repositories.TierInventoryRepo;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logique métier des réservations (CdC §3.3, §3.4, FR-007, FR-008, FR-010, FR-012).
 *
 * RÔLE :
 *  - Vérifier l'événement et les paliers via event-service (OpenFeign + Resilience4J).
 *  - Réserver atomiquement les places via TierInventoryRepo (UPDATE conditionnel atomique).
 *  - Gérer l'idempotence via la table idempotency_keys (header Idempotency-Key).
 *  - Snapshotter le prix et le titre de l'événement au moment de la réservation.
 *  - Calculer le TTL de la réservation PENDING (hold-minutes dans application.yml).
 *
 * Cycle de vie :
 *  PENDING  ──confirm──▶  CONFIRMED  ──cancel──▶  CANCELLED
 *     │                       │
 *     └──expire──▶  EXPIRED    └──refund──▶  REFUNDED
 */
@Service
@Transactional
public class BookingService {

    private final BookingRepo bookingRepo;
    private final BookingLineRepo lineRepo;
    private final TierInventoryRepo inventoryRepo;
    private final IdempotencyKeyRepo idempotencyRepo;
    private final EventClient eventClient;
    private final Clock clock;

    @Value("${ticketflow.booking.hold-minutes:15}")
    private int holdMinutes;

    @Value("${ticketflow.booking.max-quantity-per-booking:10}")
    private int maxQuantityPerBooking;

    public BookingService(BookingRepo bookingRepo, BookingLineRepo lineRepo,
                          TierInventoryRepo inventoryRepo, IdempotencyKeyRepo idempotencyRepo,
                          EventClient eventClient, Clock clock) {
        this.bookingRepo = bookingRepo;
        this.lineRepo = lineRepo;
        this.inventoryRepo = inventoryRepo;
        this.idempotencyRepo = idempotencyRepo;
        this.eventClient = eventClient;
        this.clock = clock;
    }

    /**
     * Crée une réservation (FR-007, FR-008).
     *
     * Étapes :
     *  1. Idempotence : si (customerId, idempotencyKey) existe déjà, on renvoie le booking existant.
     *  2. Validation : appel event-service pour vérifier l'événement (PUBLISHED, future) + récupérer les paliers.
     *  3. Calcul du prix total (snapshot).
     *  4. Réservation atomique des places par palier (UPDATE conditionnel).
     *  5. Persistence du booking + lignes + association à la clé d'idempotence.
     */
    public Booking book(BookingRequest req, String idempotencyKey) {
        // ----- 1. Idempotence -----
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<IdempotencyKey> existing = idempotencyRepo
                    .findByCustomerIdAndIdempotencyKey(req.customerId(), idempotencyKey);
            if (existing.isPresent() && existing.get().getBookingId() != null) {
                // La réservation a déjà été créée — on la renvoie (idempotence)
                return bookingRepo.findById(existing.get().getBookingId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Booking", String.valueOf(existing.get().getBookingId())));
            }
        }

        // ----- 2. Validation via event-service -----
        EventView event = fetchEvent(req.eventId());
        if (!"PUBLISHED".equals(event.status())) {
            throw new BookingRuleException("The event is not open for booking (status=" + event.status() + ")");
        }
        Instant now = Instant.now(clock);
        if (!event.startsAt().isAfter(now)) {
            throw new BookingRuleException("The event has already started");
        }

        List<TierView> availableTiers = fetchTiers(req.eventId());

        // ----- 3. Validation des lignes + calcul du total -----
        List<BookingLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int totalQuantity = 0;
        for (LineRequest lineReq : req.lines()) {
            TierView tier = availableTiers.stream()
                    .filter(t -> t.id().equals(lineReq.ticketTierId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "TicketTier", String.valueOf(lineReq.ticketTierId())));
            if (lineReq.quantity() > maxQuantityPerBooking) {
                throw new BookingRuleException(
                        "Quantity " + lineReq.quantity() + " exceeds max per booking (" + maxQuantityPerBooking + ")");
            }
            totalQuantity += lineReq.quantity();
            // Le BookingLine sera attaché au booking plus tard (after save)
            lines.add(new BookingLine(null, lineReq.ticketTierId(), tier.name(),
                    tier.price(), lineReq.quantity()));
            total = total.add(tier.price().multiply(BigDecimal.valueOf(lineReq.quantity())));
        }
        if (totalQuantity == 0) {
            throw new BookingRuleException("Booking must contain at least one seat");
        }
        if (totalQuantity > maxQuantityPerBooking) {
            throw new BookingRuleException(
                    "Total quantity " + totalQuantity + " exceeds max per booking (" + maxQuantityPerBooking + ")");
        }

        // ----- 4. Réservation atomique des places par palier -----
        // Pour chaque palier, on s'assure que la ligne d'inventaire existe (initialisée à la première réservation),
        // puis on réserve atomiquement via UPDATE conditionnel.
        for (BookingLine line : lines) {
            ensureInventoryExists(line.getTicketTierId(), findTierCapacity(availableTiers, line.getTicketTierId()));
            int updated = inventoryRepo.reserve(line.getTicketTierId(), line.getQuantity());
            if (updated == 0) {
                throw new BookingRuleException(
                        "Not enough seats left for tier " + line.getTicketTierId());
            }
        }

        // ----- 5. Persistence du booking -----
        Instant expiresAt = now.plus(holdMinutes, ChronoUnit.MINUTES);
        String reference = generateReference(now);
        Booking booking = new Booking(reference, req.eventId(), event.title(),
                req.customerId(), total, now, expiresAt);
        booking = bookingRepo.save(booking);

        // Attache les lignes au booking persisté
        for (BookingLine line : lines) {
            BookingLine persistedLine = new BookingLine(booking, line.getTicketTierId(),
                    line.getTierName(), line.getUnitPrice(), line.getQuantity());
            lineRepo.save(persistedLine);
        }

        // ----- 6. Idempotence : on associe la clé au booking -----
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                IdempotencyKey key = new IdempotencyKey(req.customerId(), idempotencyKey,
                        now, now.plus(24, ChronoUnit.HOURS));
                key.attachBooking(booking.getId());
                idempotencyRepo.save(key);
            } catch (DataIntegrityViolationException ex) {
                // Cas très rare : la clé a été créée par une requête concurrente entre le check et l'insert.
                // On récupère le booking existant (idempotence).
                IdempotencyKey existing = idempotencyRepo
                        .findByCustomerIdAndIdempotencyKey(req.customerId(), idempotencyKey)
                        .orElseThrow(() -> ex);
                if (existing.getBookingId() != null) {
                    return bookingRepo.findById(existing.getBookingId()).orElseThrow(() -> ex);
                }
                existing.attachBooking(booking.getId());
            }
        }

        return booking;
    }

    @Transactional(readOnly = true)
    public Booking get(Long id) {
        return bookingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", String.valueOf(id)));
    }

    @Transactional(readOnly = true)
    public Booking getByReference(String reference) {
        return bookingRepo.findByBookingReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", reference));
    }

    @Transactional(readOnly = true)
    public List<BookingLine> getLines(Long bookingId) {
        return lineRepo.findByBookingId(bookingId);
    }

    @Transactional(readOnly = true)
    public Page<Booking> listByCustomer(Long customerId, Pageable pageable) {
        return bookingRepo.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
    }

    @Transactional(readOnly = true)
    public List<Booking> listAll() {
        return bookingRepo.findAllByOrderByCreatedAtDescIdDesc();
    }

    @Transactional(readOnly = true)
    public Page<Booking> listAll(Pageable pageable) {
        return bookingRepo.findAll(pageable);
    }

    /** Annuler une réservation (FR-012) — libère les places PENDING. */
    public Booking cancel(Long id) {
        Booking booking = get(id);
        Instant now = Instant.now(clock);
        booking.cancel(now);
        // Libère les places (si PENDING -> held -= quantity ; si CONFIRMED -> sold -= quantity)
        for (BookingLine line : lineRepo.findByBookingId(id)) {
            if (booking.getStatus() == BookingStatus.CANCELLED
                    && booking.getConfirmedAt() == null) {
                // PENDING -> CANCELLED : libère les places held
                inventoryRepo.release(line.getTicketTierId(), line.getQuantity());
            }
        }
        return booking;
    }

    /**
     * Confirmer une réservation (FR-010) — appelée après paiement réussi.
     * Convertit les places de "held" à "sold" dans l'inventaire.
     */
    public Booking confirm(Long id) {
        Booking booking = get(id);
        Instant now = Instant.now(clock);
        booking.confirm(now);
        for (BookingLine line : lineRepo.findByBookingId(id)) {
            inventoryRepo.confirm(line.getTicketTierId(), line.getQuantity());
        }
        return booking;
    }

    /** Expirer une réservation PENDING (cron, FR-008) — libère les places held. */
    public Booking expire(Long id) {
        Booking booking = get(id);
        Instant now = Instant.now(clock);
        booking.expire(now);
        for (BookingLine line : lineRepo.findByBookingId(id)) {
            inventoryRepo.release(line.getTicketTierId(), line.getQuantity());
        }
        return booking;
    }

    /** Liste des PENDING échues pour le cron d'expiration (FR-008). */
    @Transactional(readOnly = true)
    public List<Booking> findExpiredPending() {
        return bookingRepo.findExpiredPending(BookingStatus.PENDING, Instant.now(clock));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private EventView fetchEvent(Long eventId) {
        try {
            return eventClient.getEvent(eventId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Event", String.valueOf(eventId));
        } catch (FeignException e) {
            throw new UpstreamUnavailableException("event-service", e);
        }
    }

    private List<TierView> fetchTiers(Long eventId) {
        try {
            return eventClient.getTiers(eventId);
        } catch (FeignException e) {
            throw new UpstreamUnavailableException("event-service", e);
        }
    }

    private int findTierCapacity(List<TierView> tiers, Long tierId) {
        return tiers.stream()
                .filter(t -> t.id().equals(tierId))
                .map(TierView::quantityAvailable)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("TicketTier", String.valueOf(tierId)));
    }

    private void ensureInventoryExists(Long ticketTierId, int capacity) {
        if (!inventoryRepo.existsById(ticketTierId)) {
            try {
                inventoryRepo.save(new TierInventory(ticketTierId, capacity));
            } catch (DataIntegrityViolationException ex) {
                // Une requête concurrente a déjà créé la ligne — c'est OK, on continue
            }
        }
    }

    private String generateReference(Instant when) {
        return "BK-" + when.toString().substring(0, 10).replace("-", "") + "-"
                + Long.toHexString(System.nanoTime()).toUpperCase();
    }
}

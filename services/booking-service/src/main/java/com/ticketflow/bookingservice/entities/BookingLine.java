package com.ticketflow.bookingservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Ligne de réservation (CdC §7.1 : BookingLine).
 *
 * Une réservation peut contenir plusieurs paliers (TicketTier) en quantités différentes.
 * Chaque ligne snapshot le prix unitaire au moment de la réservation (CdC §7.1).
 *
 * {@code @ManyToOne} est LAZY (Booking est chargé explicitement, pas l'inverse).
 */
@Entity
@Table(name = "booking_lines",
        indexes = {
                @Index(name = "idx_blines_booking", columnList = "booking_id"),
                @Index(name = "idx_blines_tier", columnList = "ticket_tier_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /** Référence au palier de tarif côté event-service (pas de FK cross-bases). */
    @Column(name = "ticket_tier_id", nullable = false)
    private Long ticketTierId;

    /** Snapshot du nom du palier au moment de la réservation. */
    @Column(name = "tier_name", nullable = false, length = 80)
    private String tierName;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    /** JPA constructor. Le total est calculé à la création. */
    public BookingLine(Booking booking, Long ticketTierId, String tierName,
                       BigDecimal unitPrice, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1");
        }
        this.booking = booking;
        this.ticketTierId = ticketTierId;
        this.tierName = tierName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getLineTotal() { return lineTotal; }
}

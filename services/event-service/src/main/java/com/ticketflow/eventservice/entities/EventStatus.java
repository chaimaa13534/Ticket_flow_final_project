package com.ticketflow.eventservice.entities;

/**
 * Cycle de vie d'un événement (CdC §3.6 et FR-004).
 *
 * Transitions autorisées :
 *
 *   DRAFT  ──publish──▶  PUBLISHED  ──close──▶  CLOSED  ──archive──▶  ARCHIVED
 *     │                     │                       │
 *     └──cancel──▶          └──cancel──▶            └──cancel──▶
 *                     CANCELLED
 *
 * Règles métier :
 *  - DRAFT     : brouillon, jamais exposé publiquement (CdC FR-003).
 *  - PUBLISHED : visible dans le catalogue public, peut recevoir des réservations.
 *  - CLOSED    : plus réservable mais reste visible dans l'historique.
 *  - ARCHIVED  : masqué du catalogue public, conservé pour audit.
 *  - CANCELLED : annulé, places remboursées selon politique (CdC FR-012).
 */
public enum EventStatus {
    DRAFT,
    PUBLISHED,
    CLOSED,
    ARCHIVED,
    CANCELLED
}

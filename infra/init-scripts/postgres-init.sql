-- ============================================================================
-- TicketFlow — Initialisation PostgreSQL
-- Exécuté par Docker Compose au premier démarrage du conteneur postgres.
-- Crée une base de données séparée PAR service métier (CdC §7.1, §8.1).
-- Aucune jointure inter-bases n'est autorisée.
-- ============================================================================

CREATE DATABASE ticketflow_customer;
CREATE DATABASE ticketflow_event;
CREATE DATABASE ticketflow_booking;
CREATE DATABASE ticketflow_payment;
CREATE DATABASE ticketflow_ticket;
CREATE DATABASE ticketflow_notification;
CREATE DATABASE ticketflow_assistant;

-- Le user "ticketflow" (créé par POSTGRES_USER dans le .env) hérite des droits par défaut.
-- Les schémas sont créés par Flyway dans chaque service au démarrage.

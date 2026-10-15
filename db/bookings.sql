-- ============================================================
-- Booking - entsteht aus einer bezahlten Reservation
-- (POST /payment, success=true, innerhalb der 30 Minuten)
--
--   reservations 1:1 bookings
--
-- Regeln:
--   * booking_number wird in der Anwendung erzeugt
--     (BOOK-YYYYMMDD-XXXXXX) und ist per UNIQUE-Constraint
--     kollisionssicher.
--   * Der Uebergang PENDING -> PAID erfolgt atomar im selben
--     Statement, das die Booking anlegt (nur solange die Reservation
--     laut DB-Uhr noch PENDING und nicht abgelaufen ist).
--   * Eine PAID-Reservation gibt ihre Availability NIE zurueck:
--     Release passiert nur bei PENDING -> EXPIRED/CANCELLED.
--   * on delete restrict: eine Booking schuetzt ihre Reservation
--     vor versehentlichem Loeschen.
--
-- ============================================================

create table bookings (
    id             uuid primary key default gen_random_uuid(),
    booking_number text not null unique,

    reservation_id uuid not null unique
                   constraint fk_bookings_reservation
                   references reservations (id) on delete restrict,

    paid_at        timestamptz not null default now(),
    created_at     timestamptz not null default now()
);

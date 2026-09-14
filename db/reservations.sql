-- ============================================================
-- Reservation-Phase – Supabase/PostgreSQL Schema (MVP)
--
-- Zwei Tabellen:
--   reservations          – Kopf der Reservation (Reise, Personen,
--                           eingefrorene Preise, Status, Ablauf)
--   reservation_services  – eine Zeile je Leistung
--                           (FLIGHT | RETURN_FLIGHT | HOTEL | CAR)
--
-- Regeln:
--   * Reservation ist 30 Minuten gültig (expires_at).
--   * Preise/Zeiten sind Snapshots; Hotel-/Flug-/Car-Inhalte werden
--     über service_id (Mock-API-IDs) aufgelöst.
--   * reservation_number wird in der Anwendung erzeugt
--     (RES-YYYYMMDD-XXXXXX, 6 Zeichen aus 32er-Alphabet ohne I/O/0/1)
--     und ist per UNIQUE-Constraint kollisionssicher.
--   * total_price wird vom Backend berechnet und beim Anlegen gegen
--     die Summe der reservation_services.price validiert.
--     Die DB erzwingt die Summen-Gleichheit im MVP NICHT; Härtung
--     später per BEFORE INSERT/UPDATE-Trigger (Summe der Services).
--   * "RETURN_FLIGHT ohne FLIGHT" wird in der App validiert
--     (kein Trigger im MVP).
-- ============================================================

create table reservations (
    id                 uuid primary key default gen_random_uuid(),
    reservation_number text not null unique,

    -- PENDING | EXPIRED | CANCELLED  (PAID entsteht erst mit dem Booking)
    status             text not null default 'PENDING'
                       constraint chk_reservations_status
                       check (status in ('PENDING', 'EXPIRED', 'CANCELLED')),

    -- Reisedaten (Snapshot)
    origin             text not null,
    destination        text not null,
    departure_date     date not null,
    return_date        date,                       -- null = keine Rückreise

    -- Reisende (Snapshot)
    adults             int not null default 1,
    children           int not null default 0,
    infants            int not null default 0,

    -- Preis (Snapshot; vom Backend berechnet = Summe aller
    -- reservation_services.price, App-Validierung beim Anlegen)
    currency           text not null
                       constraint chk_reservations_currency
                       check (currency ~ '^[A-Z]{3}$'),
    total_price        numeric(12, 2) not null,

    -- Zeiten
    created_at         timestamptz not null default now(),
    expires_at         timestamptz not null default now() + interval '30 minutes',
    cancelled_at       timestamptz,

    constraint chk_reservations_adults      check (adults >= 1),
    constraint chk_reservations_children    check (children >= 0),
    constraint chk_reservations_infants     check (infants >= 0),
    constraint chk_reservations_dates       check (return_date is null or return_date >= departure_date),
    constraint chk_reservations_price       check (total_price >= 0),
    constraint chk_reservations_expiry      check (expires_at > created_at),
    constraint chk_reservations_cancelled   check (cancelled_at is null or status = 'CANCELLED')
);

-- Für den späteren Expiry-Sweep-Job (und schnelle Lazy-Checks):
create index idx_reservations_pending_expiry
    on reservations (expires_at)
    where status = 'PENDING';

create table reservation_services (
    id             uuid primary key default gen_random_uuid(),
    reservation_id uuid not null
                   constraint fk_services_reservation
                   references reservations (id) on delete cascade,

    service_type   text not null
                   constraint chk_services_type
                   check (service_type in ('FLIGHT', 'RETURN_FLIGHT', 'HOTEL', 'CAR')),

    -- Mock-API-ID, z.B. FL-1001 / HOT-1001 / CAR-1001
    service_id     text not null,
    -- Für Cars zusätzlich der Provider (Pflicht für /details/car)
    provider_id    text,

    -- Eingefrorener Preis dieser Leistung (gleiche Währung wie reservations.currency)
    price          numeric(12, 2) not null,

    -- Nur HOTEL
    check_in       date,
    check_out      date,
    room_id        text,       -- z.B. ROOM-102, gilt innerhalb des
                               -- service_id-Hotels (roomId ist nicht
                               -- global eindeutig)

    -- Nur CAR
    pickup_date    date,
    return_date    date,
    pickup_location text,
    return_location text,

    constraint chk_services_price   check (price >= 0),
    -- max. 1 Flug, 1 Rückflug, 1 Hotel, 1 Car pro Reservation
    constraint uq_services_per_type unique (reservation_id, service_type),

    -- FLIGHT / RETURN_FLIGHT: keine Hotel-/Car-Felder
    constraint chk_services_flight_fields check (
        service_type not in ('FLIGHT', 'RETURN_FLIGHT')
        or (check_in is null and check_out is null and room_id is null
            and pickup_date is null and return_date is null
            and pickup_location is null and return_location is null
            and provider_id is null)
    ),

    -- HOTEL: Check-In/Out + room_id Pflicht, keine Car-/Provider-Felder
    constraint chk_services_hotel_fields check (
        service_type <> 'HOTEL'
        or (check_in is not null
            and check_out is not null and check_out > check_in
            and room_id is not null
            and pickup_date is null and return_date is null
            and pickup_location is null and return_location is null
            and provider_id is null)
    ),

    -- CAR: Provider + Pickup Pflicht, keine Hotel-Felder
    constraint chk_services_car_fields check (
        service_type <> 'CAR'
        or (provider_id is not null
            and pickup_date is not null and return_date is not null
            and return_date >= pickup_date
            and pickup_location is not null
            and check_in is null and check_out is null and room_id is null)
    )
);

create index idx_services_reservation on reservation_services (reservation_id);

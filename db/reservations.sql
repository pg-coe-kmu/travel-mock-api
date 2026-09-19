-- ============================================================
-- Reservation-Phase - Supabase/PostgreSQL Schema
-- Flexibles Modell: generische Items + Detailtabellen pro Leistung
--
--   reservations 1:N reservation_items
--   reservation_items 1:1 reservation_flights | reservation_hotels | reservation_cars
--
-- Regeln:
--   * reservation_items enthaelt NUR gemeinsame Felder (item_type, price);
--     service-spezifische Daten (inkl. der externen Mock-API-IDs)
--     liegen in den Detailtabellen.
--   * Zeitraeume liegen auf Leistungsebene (Flight/Hotel/Car koennen
--     unterschiedliche Zeitraeume haben).
--   * Preise/Zeiten/Namen sind Snapshots; /details loest die vollen
--     Angebotsinhalte ueber die externen IDs aus der Mock-API auf.
--   * total_price wird vom Backend berechnet (Summe der Items);
--     die DB erzwingt die Summe im MVP NICHT.
--   * reservation_number wird in der Anwendung erzeugt
--     (RES-YYYYMMDD-XXXXXX) und ist per UNIQUE-Constraint kollisionssicher.
--   * Ablauf: 30 Minuten (expires_at), lazy geprueft bei Zugriff.
--
-- ============================================================

create table reservations (
    id                 uuid primary key default gen_random_uuid(),
    reservation_number text not null unique,

    -- PENDING | EXPIRED | CANCELLED  (PAID entsteht erst mit dem Booking)
    status             text not null default 'PENDING'
                       constraint chk_reservations_status
                       check (status in ('PENDING', 'EXPIRED', 'CANCELLED')),

    origin             text not null,
    destination        text not null,

    adults             int not null default 1,
    children           int not null default 0,
    infants            int not null default 0,

    -- Snapshot; vom Backend berechnet = Summe aller reservation_items.price
    currency           text not null
                       constraint chk_reservations_currency
                       check (currency ~ '^[A-Z]{3}$'),
    total_price        numeric(12, 2) not null,

    created_at         timestamptz not null default now(),
    expires_at         timestamptz not null default now() + interval '30 minutes',
    cancelled_at       timestamptz,

    constraint chk_reservations_adults      check (adults >= 1),
    constraint chk_reservations_children    check (children >= 0),
    constraint chk_reservations_infants     check (infants >= 0),
    constraint chk_reservations_price       check (total_price >= 0),
    constraint chk_reservations_expiry      check (expires_at > created_at),
    constraint chk_reservations_cancelled   check (cancelled_at is null or status = 'CANCELLED')
);

-- Fuer den spaeteren Expiry-Sweep-Job (und schnelle Lazy-Checks):
create index idx_reservations_pending_expiry
    on reservations (expires_at)
    where status = 'PENDING';

-- ------------------------------------------------------------
-- Generische Leistungs-Items: NUR gemeinsame Eigenschaften
-- ------------------------------------------------------------
create table reservation_items (
    id             uuid primary key default gen_random_uuid(),
    reservation_id uuid not null
                   constraint fk_items_reservation
                   references reservations (id) on delete cascade,

    item_type      text not null
                   constraint chk_items_type
                   check (item_type in ('FLIGHT', 'HOTEL', 'CAR')),

    -- Eingefrorener Preis dieser Leistung (gleiche Waehrung wie reservations.currency)
    price          numeric(12, 2) not null
                   constraint chk_items_price check (price >= 0),

    created_at     timestamptz not null default now()
);

create index idx_items_reservation on reservation_items (reservation_id);

-- ------------------------------------------------------------
-- Fluege (1:1 zu reservation_items)
-- Hin- UND Rueckflug = zwei Items mit direction OUTBOUND / RETURN
-- ------------------------------------------------------------
create table reservation_flights (
    reservation_item_id uuid primary key
                   constraint fk_flights_item
                   references reservation_items (id) on delete cascade,

    direction           text not null
                   constraint chk_flights_direction
                   check (direction in ('OUTBOUND', 'RETURN')),

    -- Externe Mock-API-ID, z.B. FL-1001
    flight_id           text not null,
    airline             text not null,
    flight_number       text not null,
    departure_airport   text not null,
    arrival_airport     text not null,
    departure_at        timestamptz not null,
    arrival_at          timestamptz not null,

    constraint chk_flights_times check (arrival_at > departure_at)
);

-- ------------------------------------------------------------
-- Hotels (1:1 zu reservation_items)
-- hotel_id = externe Mock-API-ID (z.B. HOT-1001),
-- reservation_item_id = interne Item-Referenz
-- ------------------------------------------------------------
create table reservation_hotels (
    reservation_item_id uuid primary key
                   constraint fk_hotels_item
                   references reservation_items (id) on delete cascade,

    hotel_id            text not null,
    room_id             text not null,
    check_in            date not null,
    check_out           date not null,

    -- Snapshots fuer die Anzeige, auch wenn sich die Mock-API aendert
    hotel_name          text not null,
    room_name           text not null,

    constraint chk_hotels_dates check (check_out > check_in)
);

-- ------------------------------------------------------------
-- Mietwagen (1:1 zu reservation_items)
-- car_id = externe Mock-API-ID (z.B. CAR-1001)
-- ------------------------------------------------------------
create table reservation_cars (
    reservation_item_id uuid primary key
                   constraint fk_cars_item
                   references reservation_items (id) on delete cascade,

    car_id              text not null,
    provider_id         text not null,
    pickup_at           timestamptz not null,
    return_at           timestamptz not null,
    pickup_location     text not null,
    return_location     text,
    vehicle_name        text not null,

    constraint chk_cars_times check (return_at >= pickup_at)
);

-- ------------------------------------------------------------
-- Availability-Holds: eine Zeile pro reserviertem Katalog-Item.
--
-- Beim Anlegen der Reservation wird die Katalog-Availability
-- (flights.available_seats, room_types.available_rooms,
-- cars.available_vehicles) atomar um 1 dekrementiert; schlaegt das
-- fehl, rollt die ganze Reservation zurueck.
--
-- Rueckgabe: genau einmal, nur beim gewinnenden Statuswechsel
-- PENDING -> EXPIRED (30-Minuten-Ablauf) bzw. PENDING -> CANCELLED.
-- Der Claim (`update ... where not restored`) verhindert doppelte
-- Rueckgabe auch bei konkurrierenden Laeufen. Bezahlte /
-- weiterverarbeitete Reservationen sind nicht mehr PENDING und
-- geben ihre Availability daher nie zurueck.
--
-- extern_id zeigt auf die Katalogzeile (bewusst ohne FK: die
-- Reservationstabellen referenzieren den Katalog nur per Text-ID).
-- ------------------------------------------------------------
create table reservation_availability (
    reservation_item_id uuid primary key
                   constraint fk_availability_item
                   references reservation_items (id) on delete cascade,

    catalog_table      text not null
                   constraint chk_availability_catalog
                   check (catalog_table in ('flights', 'room_types', 'cars')),

    external_id        text not null,

    restored           boolean not null default false,
    restored_at        timestamptz
);

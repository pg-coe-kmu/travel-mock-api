-- ============================================================
-- Phase 1 - Katalog-/Mock-Daten (Supabase/PostgreSQL)
-- Struktur 1:1 aus den JSON-Dateien (data/) und Java-Models abgeleitet.
--
--   hotels 1:N room_types
--   car_providers 1:N cars
--   car_locations 1:N cars (pickup/return, dedupliziert per external_id)
--   cars 1:N car_extras
--   flights N:1 airports (departure/arrival via external_id = IATA-Code)
--
-- Regeln (analog zu reservations.sql):
--   * id            = technische UUID-PK
--   * external_id   = bestehende Mock-ID aus den JSON-Dateien, UNIQUE
--   * Geld          = numeric(12,2), Zeiten = timestamptz
--   * Listen        = text[] (Amenities, Services, Zahlungsarten)
--   * Der Seeder (CatalogSeeder) schreibt nur in leere Tabellen;
--     die Mock-API liest in Phase 1 weiter aus dem In-Memory-Bestand.
--   * Reset/Reseed laeuft ausschliesslich ueber POST /admin/catalog/reset;
--     die Reservationstabellen bleiben davon unberuehrt (keine FKs hierher).
--
-- ============================================================

-- ------------------------------------------------------------
-- Airports: external_id = IATA-Code (z.B. BCN)
-- ------------------------------------------------------------
create table airports (
    id          uuid primary key default gen_random_uuid(),
    external_id text not null unique,
    name        text not null,
    location    text not null
);

-- ------------------------------------------------------------
-- Hotels: external_id = z.B. HOT-1001
-- ------------------------------------------------------------
create table hotels (
    id                 uuid primary key default gen_random_uuid(),
    external_id        text not null unique,

    name               text not null,
    city               text not null,
    country            text not null,
    address            text,

    stars              int not null
                       constraint chk_hotels_stars check (stars between 1 and 5),

    rating_score       numeric(4, 2)
                       constraint chk_hotels_rating check (rating_score between 0 and 5),
    rating_review_count int not null default 0,

    amenities          text[] not null default '{}',

    check_in_time      text,
    check_out_time     text,

    base_currency      text not null
                       constraint chk_hotels_currency check (base_currency ~ '^[A-Z]{3}$')
);

-- ------------------------------------------------------------
-- Room Types: external_id = z.B. ROOM-101
-- ------------------------------------------------------------
create table room_types (
    id                 uuid primary key default gen_random_uuid(),
    hotel_id           uuid not null
                       constraint fk_rooms_hotel references hotels (id) on delete cascade,
    external_id        text not null unique,

    room_type          text not null,
    board              text,

    price_per_night    numeric(12, 2) not null
                       constraint chk_rooms_price check (price_per_night >= 0),
    available_rooms    int not null default 0
                       constraint chk_rooms_available check (available_rooms >= 0),

    max_occupancy_adults   int not null
                       constraint chk_rooms_adults check (max_occupancy_adults >= 1),
    max_occupancy_children int not null default 0
                       constraint chk_rooms_children check (max_occupancy_children >= 0),

    bed_type           text,
    room_size_sqm      int,

    free_cancellation  boolean not null default false,
    cancellation_deadline_type  text,
    cancellation_deadline_value int,
    cancellation_deadline_unit  text,

    amenities          text[] not null default '{}'
);

create index idx_rooms_hotel on room_types (hotel_id);

-- ------------------------------------------------------------
-- Flights: external_id = z.B. FL-1001
-- departure/arrival_airport verweisen auf airports.external_id (IATA)
-- ------------------------------------------------------------
create table flights (
    id               uuid primary key default gen_random_uuid(),
    external_id      text not null unique,

    airline          text not null,
    flight_number    text not null,

    departure_airport text not null
                     constraint fk_flights_dep_airport references airports (external_id),
    arrival_airport   text not null
                     constraint fk_flights_arr_airport references airports (external_id),

    departure_time   timestamptz not null,
    arrival_time     timestamptz not null,

    travel_class     text not null
                     constraint chk_flights_class
                     check (travel_class in ('Economy', 'Premium Economy', 'Business', 'First')),

    price            numeric(12, 2) not null
                     constraint chk_flights_price check (price >= 0),
    currency         text not null
                     constraint chk_flights_currency check (currency ~ '^[A-Z]{3}$'),

    available_seats  int not null default 0
                     constraint chk_flights_seats check (available_seats >= 0),

    constraint chk_flights_times check (arrival_time > departure_time)
);

-- ------------------------------------------------------------
-- Car Providers: external_id = z.B. PROV-SIXT-01
-- ------------------------------------------------------------
create table car_providers (
    id                 uuid primary key default gen_random_uuid(),
    external_id        text not null unique,

    provider_name      text not null,

    rating_score       numeric(4, 2)
                       constraint chk_providers_rating check (rating_score between 0 and 5),
    rating_review_count int not null default 0,

    base_currency      text not null
                       constraint chk_providers_currency check (base_currency ~ '^[A-Z]{3}$'),

    min_driver_age         int not null default 18,
    young_driver_fee_per_day numeric(12, 2),
    deposit_amount         numeric(12, 2),
    accepted_payment_methods text[] not null default '{}'
);

-- ------------------------------------------------------------
-- Car Locations: external_id = z.B. LOC-BCN-AP
-- (in cars.json pro Auto dupliziert - Seeder dedupliziert per external_id)
-- ------------------------------------------------------------
create table car_locations (
    id            uuid primary key default gen_random_uuid(),
    external_id   text not null unique,

    name          text not null,
    city          text not null,
    address       text,
    opening_hours text
);

-- ------------------------------------------------------------
-- Cars: external_id = z.B. CAR-1001
-- ------------------------------------------------------------
create table cars (
    id                uuid primary key default gen_random_uuid(),
    provider_id       uuid not null
                      constraint fk_cars_provider references car_providers (id) on delete cascade,
    external_id       text not null unique,

    vehicle_class     text,
    category_code     text,
    brand             text,
    model             text,

    available_vehicles int not null default 0
                      constraint chk_cars_available check (available_vehicles >= 0),

    -- specifications
    transmission      text,
    fuel_type         text,
    doors             int,
    seats             int,
    luggage_large_bags int,
    luggage_small_bags int,
    air_condition     boolean not null default false,
    drive_type        text,

    -- pricing
    price_per_day     numeric(12, 2) not null
                      constraint chk_cars_price check (price_per_day >= 0),
    total_price       numeric(12, 2)
                      constraint chk_cars_total_price check (total_price >= 0),
    rental_days       int,

    included_services text[] not null default '{}',

    free_cancellation boolean not null default false,
    cancellation_deadline_type  text,
    cancellation_deadline_value int,
    cancellation_deadline_unit  text,

    pickup_location_id uuid not null
                       constraint fk_cars_pickup references car_locations (id),
    return_location_id uuid not null
                       constraint fk_cars_return references car_locations (id)
);

create index idx_cars_provider on cars (provider_id);

-- ------------------------------------------------------------
-- Car Extras: external_id = z.B. EXT-GPS (eindeutig pro Auto)
-- ------------------------------------------------------------
create table car_extras (
    id            uuid primary key default gen_random_uuid(),
    car_id        uuid not null
                  constraint fk_extras_car references cars (id) on delete cascade,
    external_id   text not null,

    name          text not null,
    price_per_day numeric(12, 2),
    price_type    text,

    constraint uq_extras_car_external unique (car_id, external_id)
);

create index idx_extras_car on car_extras (car_id);

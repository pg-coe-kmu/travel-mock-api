package com.coe.b04.server.repository;

import com.coe.b04.server.model.AdditionalExtra;
import com.coe.b04.server.model.Airport;
import com.coe.b04.server.model.Car;
import com.coe.b04.server.model.CarLocation;
import com.coe.b04.server.model.CarProvider;
import com.coe.b04.server.model.Flight;
import com.coe.b04.server.model.Hotel;
import com.coe.b04.server.model.RoomType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/*
 * JDBC-Schreibzugriff auf die Katalogtabellen (Schema: db/catalog.sql).
 * Wird in Phase 1 ausschliesslich vom CatalogSeeder (Startup + Admin-Reset)
 * verwendet; die Mock-API liest weiter aus den In-Memory-Repositories.
 */
@Repository
public class CatalogRepository {

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    public CatalogRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = jdbcClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---------- Leer-Checks (Seeder-Idempotenz pro Top-Level-Tabelle) ----------

    public long countAirports() {
        return jdbcClient.sql("select count(*) from airports").query(Long.class).single();
    }

    public long countFlights() {
        return jdbcClient.sql("select count(*) from flights").query(Long.class).single();
    }

    public long countHotels() {
        return jdbcClient.sql("select count(*) from hotels").query(Long.class).single();
    }

    public long countProviders() {
        return jdbcClient.sql("select count(*) from car_providers").query(Long.class).single();
    }

    // ---------- Inserts ----------

    public void insertAirport(Airport airport) {
        jdbcClient.sql("""
                        insert into airports (external_id, name, location)
                        values (?, ?, ?)
                        """)
                .param(airport.getIata_code())
                .param(airport.getName())
                .param(airport.getLocation())
                .update();
    }

    public UUID insertHotel(Hotel hotel) {
        return jdbcClient.sql("""
                        insert into hotels (
                            external_id, name, city, country, address, stars,
                            rating_score, rating_review_count, amenities,
                            check_in_time, check_out_time, base_currency)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        returning id
                        """)
                .param(hotel.getHotelId())
                .param(hotel.getName())
                .param(hotel.getCity())
                .param(hotel.getCountry())
                .param(hotel.getAddress())
                .param(hotel.getStars())
                .param(hotel.getRating() == null ? null : hotel.getRating().getScore())
                .param(hotel.getRating() == null ? 0 : hotel.getRating().getReviewCount())
                .param(toArray(hotel.getHotelAmenities()))
                .param(hotel.getCheckInTime())
                .param(hotel.getCheckOutTime())
                .param(hotel.getBaseCurrency())
                .query(UUID.class)
                .single();
    }

    public void insertRoomType(UUID hotelId, RoomType room) {
        jdbcClient.sql("""
                        insert into room_types (
                            hotel_id, external_id, room_type, board,
                            price_per_night, available_rooms,
                            max_occupancy_adults, max_occupancy_children,
                            bed_type, room_size_sqm,
                            free_cancellation,
                            cancellation_deadline_type, cancellation_deadline_value, cancellation_deadline_unit,
                            amenities)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)
                .param(hotelId)
                .param(room.getRoomId())
                .param(room.getRoomType())
                .param(room.getBoard())
                .param(room.getPricePerNight())
                .param(room.getAvailableRooms())
                .param(room.getMaxOccupancy() == null ? 1 : room.getMaxOccupancy().getAdults())
                .param(room.getMaxOccupancy() == null ? 0 : room.getMaxOccupancy().getChildren())
                .param(room.getBedType())
                .param(room.getRoomSizeSqm())
                .param(room.getCancellationPolicy() != null && room.getCancellationPolicy().isFreeCancellation())
                .param(room.getCancellationPolicy() == null || room.getCancellationPolicy().getCancellationDeadline() == null
                        ? null : room.getCancellationPolicy().getCancellationDeadline().getType())
                .param(room.getCancellationPolicy() == null || room.getCancellationPolicy().getCancellationDeadline() == null
                        ? null : room.getCancellationPolicy().getCancellationDeadline().getValue())
                .param(room.getCancellationPolicy() == null || room.getCancellationPolicy().getCancellationDeadline() == null
                        ? null : room.getCancellationPolicy().getCancellationDeadline().getUnit())
                .param(toArray(room.getRoomAmenities()))
                .update();
    }

    public void batchInsertFlights(List<Flight> flights) {
        jdbcTemplate.batchUpdate("""
                        insert into flights (
                            external_id, airline, flight_number,
                            departure_airport, arrival_airport,
                            departure_time, arrival_time,
                            travel_class, price, currency, available_seats)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                flights, 500, (ps, flight) -> {
                    ps.setString(1, flight.getFlightId());
                    ps.setString(2, flight.getAirline());
                    ps.setString(3, flight.getFlightNumber());
                    ps.setString(4, flight.getDepartureAirport());
                    ps.setString(5, flight.getArrivalAirport());
                    ps.setObject(6, flight.getDepartureTime().atOffset(ZoneOffset.UTC));
                    ps.setObject(7, flight.getArrivalTime().atOffset(ZoneOffset.UTC));
                    ps.setString(8, flight.getTravelClass().getName());
                    ps.setBigDecimal(9, flight.getPrice());
                    ps.setString(10, flight.getCurrency());
                    ps.setInt(11, flight.getAvailableSeats());
                });
    }

    public UUID insertProvider(CarProvider provider) {
        return jdbcClient.sql("""
                        insert into car_providers (
                            external_id, provider_name,
                            rating_score, rating_review_count, base_currency,
                            min_driver_age, young_driver_fee_per_day, deposit_amount,
                            accepted_payment_methods)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        returning id
                        """)
                .param(provider.getProviderId())
                .param(provider.getProviderName())
                .param(provider.getRating() == null ? null : provider.getRating().getScore())
                .param(provider.getRating() == null ? 0 : provider.getRating().getReviewCount())
                .param(provider.getBaseCurrency())
                .param(provider.getProviderPolicies().getMinDriverAge())
                .param(provider.getProviderPolicies().getYoungDriverFeePerDay())
                .param(provider.getProviderPolicies().getDepositAmount())
                .param(toArray(provider.getProviderPolicies().getAcceptedPaymentMethods()))
                .query(UUID.class)
                .single();
    }

    public UUID insertLocation(CarLocation location) {
        return jdbcClient.sql("""
                        insert into car_locations (external_id, name, city, address, opening_hours)
                        values (?, ?, ?, ?, ?)
                        returning id
                        """)
                .param(location.getLocationId())
                .param(location.getName())
                .param(location.getCity())
                .param(location.getAddress())
                .param(location.getOpeningHours())
                .query(UUID.class)
                .single();
    }

    public UUID insertCar(UUID providerId, UUID pickupLocationId, UUID returnLocationId, Car car) {
        return jdbcClient.sql("""
                        insert into cars (
                            provider_id, external_id,
                            vehicle_class, category_code, brand, model,
                            available_vehicles,
                            transmission, fuel_type, doors, seats,
                            luggage_large_bags, luggage_small_bags, air_condition, drive_type,
                            price_per_day, total_price, rental_days,
                            included_services,
                            free_cancellation,
                            cancellation_deadline_type, cancellation_deadline_value, cancellation_deadline_unit,
                            pickup_location_id, return_location_id)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        returning id
                        """)
                .param(providerId)
                .param(car.getCarId())
                .param(car.getVehicleClass())
                .param(car.getCategoryCode())
                .param(car.getBrand())
                .param(car.getModel())
                .param(car.getAvailableVehicles())
                .param(car.getSpecifications().getTransmission())
                .param(car.getSpecifications().getFuelType())
                .param(car.getSpecifications().getDoors())
                .param(car.getSpecifications().getSeats())
                .param(car.getSpecifications().getLuggageCapacity().getLargeBags())
                .param(car.getSpecifications().getLuggageCapacity().getSmallBags())
                .param(car.getSpecifications().isAirCondition())
                .param(car.getSpecifications().getDriveType())
                .param(car.getPricing().getPricePerDay())
                .param(car.getPricing().getTotalPrice())
                .param(car.getPricing().getRentalDays())
                .param(toArray(car.getIncludedServices()))
                .param(car.getCancellationPolicy() != null && car.getCancellationPolicy().isFreeCancellation())
                .param(car.getCancellationPolicy() == null || car.getCancellationPolicy().getCancellationDeadline() == null
                        ? null : car.getCancellationPolicy().getCancellationDeadline().getType())
                .param(car.getCancellationPolicy() == null || car.getCancellationPolicy().getCancellationDeadline() == null
                        ? null : car.getCancellationPolicy().getCancellationDeadline().getValue())
                .param(car.getCancellationPolicy() == null || car.getCancellationPolicy().getCancellationDeadline() == null
                        ? null : car.getCancellationPolicy().getCancellationDeadline().getUnit())
                .param(pickupLocationId)
                .param(returnLocationId)
                .query(UUID.class)
                .single();
    }

    public void insertExtra(UUID carId, AdditionalExtra extra) {
        jdbcClient.sql("""
                        insert into car_extras (car_id, external_id, name, price_per_day, price_type)
                        values (?, ?, ?, ?, ?)
                        """)
                .param(carId)
                .param(extra.getExtraId())
                .param(extra.getName())
                .param(extra.getPricePerDay())
                .param(extra.getPriceType())
                .update();
    }

    // ---------- Reset (nur Katalog, Reservationsdaten bleiben unberuehrt) ----------

    public void deleteAllCatalogData() {
        jdbcTemplate.execute("""
                truncate table car_extras, cars, car_locations, car_providers,
                             room_types, hotels, flights, airports
                restart identity cascade
                """);
    }

    private String[] toArray(List<String> values) {
        return values == null ? new String[0] : values.toArray(String[]::new);
    }
}

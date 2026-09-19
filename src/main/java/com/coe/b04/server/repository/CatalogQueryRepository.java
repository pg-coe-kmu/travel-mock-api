package com.coe.b04.server.repository;

import com.coe.b04.server.enums.TravelClass;
import com.coe.b04.server.model.AdditionalExtra;
import com.coe.b04.server.model.CancellationDeadline;
import com.coe.b04.server.model.CancellationPolicy;
import com.coe.b04.server.model.Car;
import com.coe.b04.server.model.CarLocation;
import com.coe.b04.server.model.CarLocations;
import com.coe.b04.server.model.CarPricing;
import com.coe.b04.server.model.CarProvider;
import com.coe.b04.server.model.CarSpecifications;
import com.coe.b04.server.model.Flight;
import com.coe.b04.server.model.Hotel;
import com.coe.b04.server.model.LuggageCapacity;
import com.coe.b04.server.model.MaxOccupancy;
import com.coe.b04.server.model.ProviderPolicies;
import com.coe.b04.server.model.Rating;
import com.coe.b04.server.model.RoomType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
 * DB-Read-Seite der Katalogtabellen (Schema: db/catalog.sql): laedt die
 * kompletten Katalogmodelle (Hotels inkl. RoomTypes, Fluege, Provider inkl.
 * Cars/Locations/Extras) aus PostgreSQL. Die Katalog-Repositories (Hotel-,
 * Flight-, Car-, AirportRepository) lesen ausschliesslich hierueber; ihre
 * Filterlogik laeuft in Java ueber die DB-geladenen Listen.
 *
 * Availability (available_rooms/seats/vehicles) kommt damit direkt aus der
 * DB - Dekremente/Rueckgaben der Reservation sind in allen Read-Pfaden
 * sofort sichtbar.
 */
@Repository
public class CatalogQueryRepository {

    private final JdbcClient jdbcClient;

    public CatalogQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    // ---------- Hotels + RoomTypes ----------

    public List<Hotel> findAllHotels() {
        Map<String, Hotel> hotelsByExternalId = new HashMap<>();
        jdbcClient.sql("""
                        select external_id, name, city, country, address, stars,
                               rating_score, rating_review_count, amenities,
                               check_in_time, check_out_time, base_currency
                        from hotels
                        """)
                .query((rs, rowNum) -> {
                    BigDecimal score = rs.getBigDecimal("rating_score");
                    Hotel hotel = Hotel.builder()
                            .hotelId(rs.getString("external_id"))
                            .name(rs.getString("name"))
                            .city(rs.getString("city"))
                            .country(rs.getString("country"))
                            .address(rs.getString("address"))
                            .stars(rs.getInt("stars"))
                            .rating(score == null
                                    ? null : new Rating(score, rs.getInt("rating_review_count")))
                            .hotelAmenities(toList(rs, "amenities"))
                            .checkInTime(rs.getString("check_in_time"))
                            .checkOutTime(rs.getString("check_out_time"))
                            .baseCurrency(rs.getString("base_currency"))
                            .roomTypes(new ArrayList<>())
                            .build();
                    hotelsByExternalId.put(hotel.getHotelId(), hotel);
                    return hotel;
                })
                .list();

        jdbcClient.sql("""
                        select r.external_id, h.external_id as hotel_external_id, r.room_type, r.board,
                               r.price_per_night, r.available_rooms,
                               r.max_occupancy_adults, r.max_occupancy_children,
                               r.bed_type, r.room_size_sqm,
                               r.free_cancellation,
                               r.cancellation_deadline_type, r.cancellation_deadline_value,
                               r.cancellation_deadline_unit, r.amenities
                        from room_types r
                        join hotels h on h.id = r.hotel_id
                        """)
                .query((rs, rowNum) -> new HotelRoomRow(
                        rs.getString("hotel_external_id"), roomType(rs)))
                .list()
                .forEach(row -> hotelsByExternalId.get(row.hotelExternalId())
                        .getRoomTypes().add(row.room()));

        return List.copyOf(hotelsByExternalId.values());
    }

    private RoomType roomType(ResultSet rs) throws SQLException {
        CancellationPolicy policy = cancellationPolicy(rs);
        RoomType room = new RoomType();
        room.setRoomId(rs.getString("external_id"));
        room.setRoomType(rs.getString("room_type"));
        room.setBoard(rs.getString("board"));
        room.setPricePerNight(rs.getBigDecimal("price_per_night"));
        room.setAvailableRooms(rs.getInt("available_rooms"));
        room.setMaxOccupancy(new MaxOccupancy(
                rs.getInt("max_occupancy_adults"), rs.getInt("max_occupancy_children")));
        room.setBedType(rs.getString("bed_type"));
        room.setRoomSizeSqm(rs.getInt("room_size_sqm"));
        room.setCancellationPolicy(policy);
        room.setRoomAmenities(toList(rs, "amenities"));
        return room;
    }

    private CancellationPolicy cancellationPolicy(ResultSet rs) throws SQLException {
        String deadlineType = rs.getString("cancellation_deadline_type");
        CancellationDeadline deadline = deadlineType == null
                ? null
                : new CancellationDeadline(deadlineType,
                        rs.getInt("cancellation_deadline_value"),
                        rs.getString("cancellation_deadline_unit"));
        if (deadline == null && !rs.getBoolean("free_cancellation")) {
            return null;
        }
        return new CancellationPolicy(rs.getBoolean("free_cancellation"), deadline);
    }

    // ---------- Fluege ----------

    public List<Flight> findAllFlights() {
        return jdbcClient.sql("""
                        select external_id, airline, flight_number,
                               departure_airport, arrival_airport,
                               departure_time, arrival_time,
                               travel_class, price, currency, available_seats
                        from flights
                        """)
                .query((rs, rowNum) -> {
                    Flight flight = new Flight();
                    flight.setFlightId(rs.getString("external_id"));
                    flight.setAirline(rs.getString("airline"));
                    flight.setFlightNumber(rs.getString("flight_number"));
                    flight.setDepartureAirport(rs.getString("departure_airport"));
                    flight.setArrivalAirport(rs.getString("arrival_airport"));
                    flight.setDepartureTime(
                            rs.getObject("departure_time", OffsetDateTime.class).toLocalDateTime());
                    flight.setArrivalTime(
                            rs.getObject("arrival_time", OffsetDateTime.class).toLocalDateTime());
                    flight.setTravelClass(TravelClass.fromValue(rs.getString("travel_class")));
                    flight.setPrice(rs.getBigDecimal("price"));
                    flight.setCurrency(rs.getString("currency"));
                    flight.setAvailableSeats(rs.getInt("available_seats"));
                    return flight;
                })
                .list();
    }

    // ---------- Provider + Locations + Cars + Extras ----------

    public List<CarProvider> findAllProviders() {
        Map<UUID, CarLocation> locationsById = new HashMap<>();
        jdbcClient.sql("select id, external_id, name, city, address, opening_hours from car_locations")
                .query((rs, rowNum) -> {
                    CarLocation location = new CarLocation();
                    location.setLocationId(rs.getString("external_id"));
                    location.setName(rs.getString("name"));
                    location.setCity(rs.getString("city"));
                    location.setAddress(rs.getString("address"));
                    location.setOpeningHours(rs.getString("opening_hours"));
                    locationsById.put(rs.getObject("id", UUID.class), location);
                    return location;
                })
                .list();

        Map<String, CarProvider> providersByExternalId = new HashMap<>();
        jdbcClient.sql("""
                        select external_id, provider_name,
                               rating_score, rating_review_count, base_currency,
                               min_driver_age, young_driver_fee_per_day, deposit_amount,
                               accepted_payment_methods
                        from car_providers
                        """)
                .query((rs, rowNum) -> {
                    BigDecimal score = rs.getBigDecimal("rating_score");
                    CarProvider provider = CarProvider.builder()
                            .providerId(rs.getString("external_id"))
                            .providerName(rs.getString("provider_name"))
                            .rating(score == null
                                    ? null : new Rating(score, rs.getInt("rating_review_count")))
                            .baseCurrency(rs.getString("base_currency"))
                            .providerPolicies(new ProviderPolicies(
                                    rs.getInt("min_driver_age"),
                                    rs.getBigDecimal("young_driver_fee_per_day"),
                                    rs.getBigDecimal("deposit_amount"),
                                    toList(rs, "accepted_payment_methods")))
                            .cars(new ArrayList<>())
                            .build();
                    providersByExternalId.put(provider.getProviderId(), provider);
                    return provider;
                })
                .list();

        Map<String, Car> carsByExternalId = new HashMap<>();
        jdbcClient.sql("""
                        select c.external_id, p.external_id as provider_external_id,
                               c.vehicle_class, c.category_code, c.brand, c.model,
                               c.available_vehicles,
                               c.transmission, c.fuel_type, c.doors, c.seats,
                               c.luggage_large_bags, c.luggage_small_bags, c.air_condition, c.drive_type,
                               c.price_per_day, c.total_price, c.rental_days,
                               c.included_services,
                               c.free_cancellation,
                               c.cancellation_deadline_type, c.cancellation_deadline_value,
                               c.cancellation_deadline_unit,
                               c.pickup_location_id, c.return_location_id
                        from cars c
                        join car_providers p on p.id = c.provider_id
                        """)
                .query((rs, rowNum) -> {
                    Car car = car(rs, locationsById);
                    carsByExternalId.put(car.getCarId(), car);
                    return new ProviderCarRow(rs.getString("provider_external_id"), car);
                })
                .list()
                .forEach(row -> providersByExternalId.get(row.providerExternalId())
                        .getCars().add(row.car()));

        jdbcClient.sql("""
                        select e.external_id, c.external_id as car_external_id,
                               e.name, e.price_per_day, e.price_type
                        from car_extras e
                        join cars c on c.id = e.car_id
                        """)
                .query((rs, rowNum) -> new CarExtraRow(
                        rs.getString("car_external_id"),
                        new AdditionalExtra(
                                rs.getString("external_id"),
                                rs.getString("name"),
                                rs.getBigDecimal("price_per_day"),
                                rs.getString("price_type"))))
                .list()
                .forEach(row -> carsByExternalId.get(row.carExternalId())
                        .getAdditionalExtras().add(row.extra()));

        return List.copyOf(providersByExternalId.values());
    }

    private Car car(ResultSet rs, Map<UUID, CarLocation> locationsById) throws SQLException {
        return Car.builder()
                .carId(rs.getString("external_id"))
                .vehicleClass(rs.getString("vehicle_class"))
                .categoryCode(rs.getString("category_code"))
                .brand(rs.getString("brand"))
                .model(rs.getString("model"))
                .availableVehicles(rs.getInt("available_vehicles"))
                .locations(new CarLocations(
                        locationsById.get(rs.getObject("pickup_location_id", UUID.class)),
                        locationsById.get(rs.getObject("return_location_id", UUID.class))))
                .specifications(new CarSpecifications(
                        rs.getString("transmission"),
                        rs.getString("fuel_type"),
                        rs.getInt("doors"),
                        rs.getInt("seats"),
                        new LuggageCapacity(
                                rs.getInt("luggage_large_bags"),
                                rs.getInt("luggage_small_bags")),
                        rs.getBoolean("air_condition"),
                        rs.getString("drive_type")))
                .pricing(new CarPricing(
                        rs.getBigDecimal("price_per_day"),
                        rs.getBigDecimal("total_price"),
                        rs.getInt("rental_days")))
                .includedServices(toList(rs, "included_services"))
                .cancellationPolicy(cancellationPolicy(rs))
                .additionalExtras(new ArrayList<>())
                .build();
    }

    // ---------- Airports ----------

    public String findAirportIataCodeByLocation(String location) {
        return jdbcClient.sql("select external_id from airports where lower(location) = lower(?)")
                .param(location)
                .query(String.class)
                .optional()
                .orElse(null);
    }

    private List<String> toList(ResultSet rs, String column) throws SQLException {
        java.sql.Array array = rs.getArray(column);
        return array == null ? List.of() : List.of((String[]) array.getArray());
    }

    private record HotelRoomRow(String hotelExternalId, RoomType room) {
    }

    private record ProviderCarRow(String providerExternalId, Car car) {
    }

    private record CarExtraRow(String carExternalId, AdditionalExtra extra) {
    }
}

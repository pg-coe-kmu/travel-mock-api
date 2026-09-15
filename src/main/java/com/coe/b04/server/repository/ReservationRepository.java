package com.coe.b04.server.repository;

import com.coe.b04.server.enums.Direction;
import com.coe.b04.server.enums.ReservationStatus;
import com.coe.b04.server.enums.ServiceType;
import com.coe.b04.server.model.Reservation;
import com.coe.b04.server.model.ReservationCarDetail;
import com.coe.b04.server.model.ReservationFlightDetail;
import com.coe.b04.server.model.ReservationHotelDetail;
import com.coe.b04.server.model.ReservationItem;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JDBC-Zugriff auf Supabase/PostgreSQL (Schema: db/reservations.sql).
 */
@Repository
public class ReservationRepository {

    private final JdbcClient jdbcClient;

    public ReservationRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Legt Kopf + generische Items + je eine Detailzeile in einer
     * Transaktion an. createdAt/expiresAt setzt der Aufrufer.
     */
    @Transactional
    public Reservation save(Reservation reservation) {
        UUID id = jdbcClient.sql("""
                        insert into reservations (
                            reservation_number, status, origin, destination,
                            adults, children, infants,
                            currency, total_price, created_at, expires_at, cancelled_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        returning id
                        """)
                .param(reservation.getReservationNumber())
                .param(reservation.getStatus().name())
                .param(reservation.getOrigin())
                .param(reservation.getDestination())
                .param(reservation.getAdults())
                .param(reservation.getChildren())
                .param(reservation.getInfants())
                .param(reservation.getCurrency())
                .param(reservation.getTotalPrice())
                .param(reservation.getCreatedAt())
                .param(reservation.getExpiresAt())
                .param(reservation.getCancelledAt())
                .query(UUID.class)
                .single();
        reservation.setId(id);

        // Items bekommen app-seitige, monotone created_at-Werte: now() der
        // Transaktion ist fuer alle Zeilen gleich, sonst faellt die
        // ORDER BY created_at-Sortierung auf zufaellige UUIDs zurueck.
        int index = 0;
        for (ReservationItem item : reservation.getItems()) {
            UUID itemId = jdbcClient.sql("""
                            insert into reservation_items (reservation_id, item_type, price, created_at)
                            values (?, ?, ?, ?)
                            returning id
                            """)
                    .param(id)
                    .param(item.getItemType().name())
                    .param(item.getPrice())
                    .param(reservation.getCreatedAt().plusNanos(index++ * 1_000_000L))
                    .query(UUID.class)
                    .single();
            item.setId(itemId);

            switch (item.getItemType()) {
                case FLIGHT -> insertFlight(item);
                case HOTEL -> insertHotel(item);
                case CAR -> insertCar(item);
            }
        }
        return reservation;
    }

    private void insertFlight(ReservationItem item) {
        ReservationFlightDetail flight = item.getFlight();
        jdbcClient.sql("""
                        insert into reservation_flights (
                            reservation_item_id, direction, flight_id, airline, flight_number,
                            departure_airport, arrival_airport, departure_at, arrival_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)
                .param(item.getId())
                .param(flight.getDirection().name())
                .param(flight.getFlightId())
                .param(flight.getAirline())
                .param(flight.getFlightNumber())
                .param(flight.getDepartureAirport())
                .param(flight.getArrivalAirport())
                .param(flight.getDepartureAt())
                .param(flight.getArrivalAt())
                .update();
    }

    private void insertHotel(ReservationItem item) {
        ReservationHotelDetail hotel = item.getHotel();
        jdbcClient.sql("""
                        insert into reservation_hotels (
                            reservation_item_id, hotel_id, room_id, check_in, check_out,
                            hotel_name, room_name)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """)
                .param(item.getId())
                .param(hotel.getHotelId())
                .param(hotel.getRoomId())
                .param(hotel.getCheckIn())
                .param(hotel.getCheckOut())
                .param(hotel.getHotelName())
                .param(hotel.getRoomName())
                .update();
    }

    private void insertCar(ReservationItem item) {
        ReservationCarDetail car = item.getCar();
        jdbcClient.sql("""
                        insert into reservation_cars (
                            reservation_item_id, car_id, provider_id, pickup_at, return_at,
                            pickup_location, return_location, vehicle_name)
                        values (?, ?, ?, ?, ?, ?, ?, ?)
                        """)
                .param(item.getId())
                .param(car.getCarId())
                .param(car.getProviderId())
                .param(car.getPickupAt())
                .param(car.getReturnAt())
                .param(car.getPickupLocation())
                .param(car.getReturnLocation())
                .param(car.getVehicleName())
                .update();
    }

    public Optional<Reservation> findByReservationNumber(String reservationNumber) {
        Optional<Reservation> head = jdbcClient.sql("""
                        select id, reservation_number, status, origin, destination,
                               adults, children, infants,
                               currency, total_price, created_at, expires_at, cancelled_at
                        from reservations
                        where reservation_number = ?
                        """)
                .param(reservationNumber)
                .query((rs, rowNum) -> Reservation.builder()
                        .id(rs.getObject("id", UUID.class))
                        .reservationNumber(rs.getString("reservation_number"))
                        .status(ReservationStatus.valueOf(rs.getString("status")))
                        .origin(rs.getString("origin"))
                        .destination(rs.getString("destination"))
                        .adults(rs.getInt("adults"))
                        .children(rs.getInt("children"))
                        .infants(rs.getInt("infants"))
                        .currency(rs.getString("currency"))
                        .totalPrice(rs.getBigDecimal("total_price"))
                        .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                        .expiresAt(rs.getObject("expires_at", OffsetDateTime.class))
                        .cancelledAt(rs.getObject("cancelled_at", OffsetDateTime.class))
                        .build())
                .optional();

        if (head.isEmpty()) {
            return head;
        }
        head.get().setItems(findItems(head.get().getId()));
        return head;
    }

    private List<ReservationItem> findItems(UUID reservationId) {
        List<ReservationItem> items = jdbcClient.sql("""
                        select id, item_type, price
                        from reservation_items
                        where reservation_id = ?
                        order by created_at, id
                        """)
                .param(reservationId)
                .query((rs, rowNum) -> ReservationItem.builder()
                        .id(rs.getObject("id", UUID.class))
                        .itemType(ServiceType.valueOf(rs.getString("item_type")))
                        .price(rs.getBigDecimal("price"))
                        .build())
                .list();

        if (items.isEmpty()) {
            return items;
        }

        Map<UUID, ReservationItem> byId = items.stream()
                .collect(Collectors.toMap(ReservationItem::getId, Function.identity()));

        jdbcClient.sql("""
                        select f.reservation_item_id, f.direction, f.flight_id, f.airline,
                               f.flight_number, f.departure_airport, f.arrival_airport,
                               f.departure_at, f.arrival_at
                        from reservation_flights f
                        join reservation_items i on i.id = f.reservation_item_id
                        where i.reservation_id = ?
                        """)
                .param(reservationId)
                .query((rs, rowNum) -> new FlightRow(
                        rs.getObject("reservation_item_id", UUID.class),
                        ReservationFlightDetail.builder()
                                .direction(Direction.valueOf(rs.getString("direction")))
                                .flightId(rs.getString("flight_id"))
                                .airline(rs.getString("airline"))
                                .flightNumber(rs.getString("flight_number"))
                                .departureAirport(rs.getString("departure_airport"))
                                .arrivalAirport(rs.getString("arrival_airport"))
                                .departureAt(rs.getObject("departure_at", OffsetDateTime.class))
                                .arrivalAt(rs.getObject("arrival_at", OffsetDateTime.class))
                                .build()))
                .list()
                .forEach(row -> byId.get(row.itemId()).setFlight(row.detail()));

        jdbcClient.sql("""
                        select h.reservation_item_id, h.hotel_id, h.room_id, h.check_in,
                               h.check_out, h.hotel_name, h.room_name
                        from reservation_hotels h
                        join reservation_items i on i.id = h.reservation_item_id
                        where i.reservation_id = ?
                        """)
                .param(reservationId)
                .query((rs, rowNum) -> new HotelRow(rs.getObject("reservation_item_id", UUID.class),
                        ReservationHotelDetail.builder()
                                .hotelId(rs.getString("hotel_id"))
                                .roomId(rs.getString("room_id"))
                                .checkIn(rs.getObject("check_in", LocalDate.class))
                                .checkOut(rs.getObject("check_out", LocalDate.class))
                                .hotelName(rs.getString("hotel_name"))
                                .roomName(rs.getString("room_name"))
                                .build()))
                .list()
                .forEach(row -> byId.get(row.itemId()).setHotel(row.detail()));

        jdbcClient.sql("""
                        select c.reservation_item_id, c.car_id, c.provider_id, c.pickup_at,
                               c.return_at, c.pickup_location, c.return_location, c.vehicle_name
                        from reservation_cars c
                        join reservation_items i on i.id = c.reservation_item_id
                        where i.reservation_id = ?
                        """)
                .param(reservationId)
                .query((rs, rowNum) -> new CarRow(rs.getObject("reservation_item_id", UUID.class),
                        ReservationCarDetail.builder()
                                .carId(rs.getString("car_id"))
                                .providerId(rs.getString("provider_id"))
                                .pickupAt(rs.getObject("pickup_at", OffsetDateTime.class))
                                .returnAt(rs.getObject("return_at", OffsetDateTime.class))
                                .pickupLocation(rs.getString("pickup_location"))
                                .returnLocation(rs.getString("return_location"))
                                .vehicleName(rs.getString("vehicle_name"))
                                .build()))
                .list()
                .forEach(row -> byId.get(row.itemId()).setCar(row.detail()));

        return items;
    }

    /**
     * Atomarer Statuswechsel fuer die Lazy-Expiry - wirkt nur, solange
     * die Reservation noch PENDING ist. Rueckgabe false = kein Row getroffen.
     */
    public boolean updateStatus(UUID id, ReservationStatus status) {
        int rows = jdbcClient.sql("""
                        update reservations
                        set status = ?
                        where id = ? and status = 'PENDING'
                        """)
                .param(status.name())
                .param(id)
                .update();
        return rows > 0;
    }

    /**
     * Atomarer Cancel - wirkt nur, solange die Reservation laut DB-Uhr
     * noch PENDING UND nicht abgelaufen ist (App-Uhr und DB-Uhr koennen
     * driften, expires_at entscheidet die DB). Rueckgabe false = verlor
     * gegen einen concurrenten Cancel/Expiry oder bereits abgelaufen.
     */
    public boolean cancel(UUID id, OffsetDateTime cancelledAt) {
        int rows = jdbcClient.sql("""
                        update reservations
                        set status = 'CANCELLED', cancelled_at = ?
                        where id = ? and status = 'PENDING'
                          and expires_at > CURRENT_TIMESTAMP
                        """)
                .param(cancelledAt)
                .param(id)
                .update();
        return rows > 0;
    }

    private record FlightRow(UUID itemId, ReservationFlightDetail detail) {
    }

    private record HotelRow(UUID itemId, ReservationHotelDetail detail) {
    }

    private record CarRow(UUID itemId, ReservationCarDetail detail) {
    }
}

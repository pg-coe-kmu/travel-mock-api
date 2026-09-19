package com.coe.b04.server.seeder;

import com.coe.b04.server.io.CreateReservationRequest;
import com.coe.b04.server.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Integration tests fuer Reservation x Availability gegen ein echtes
 * PostgreSQL im Testcontainer (Schemata: db/reservations.sql + db/catalog.sql,
 * Katalog via CatalogSeeder befuellt, In-Memory-Katalog via LocalBootstrap).
 *
 * Covered:
 *  - erfolgreiche Reservation dekrementiert Availability (Flight, Room, Car)
 *  - nicht ausreichende Availability -> 409, keine Reservation/Holds persistiert
 *  - Ablauf nach 30 Minuten -> Availability genau einmal zurueckgegeben
 *  - Cancel -> Availability genau einmal zurueckgegeben
 *  - nicht abgelaufene PENDING-Reservation behaelt die Availability
 *    (bezahlte/weiterverarbeitete Reservationen sind nie mehr PENDING und
 *    geben daher nie zurueck - Zahlung folgt in einer spaeteren Phase)
 *  - Race: zwei gleichzeitige Reservationen ueberbuchen den letzten
 *    verfuegbaren Bestand nicht (einer gewinnt, einer bekommt 409)
 *
 * Jeder Test setzt die Availability vorab explizit - so bleiben die Tests
 * unabhaengig von der Reihenfolge und vom Admin-Reset des Seeder-Tests.
 */
class ReservationAvailabilityIntegrationTest extends PostgresIntegrationTestBase {

    private static final String HOTEL_BODY = """
            {"origin":"Frankfurt am Main","destination":"Madrid",
             "departureDate":"2026-10-10","returnDate":"2026-10-17",
             "adults":2,"currency":"EUR",
             "hotelId":"HOT-1001","roomId":"ROOM-101"}
            """;

    private static final String FULL_BODY = """
            {"origin":"Frankfurt am Main","destination":"Madrid",
             "departureDate":"2026-10-10","returnDate":"2026-10-17",
             "adults":2,"currency":"EUR",
             "flightId":"FL-1001",
             "hotelId":"HOT-1001","roomId":"ROOM-101",
             "carId":"CAR-1001","providerId":"PROV-SIXT-01"}
            """;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationService reservationService;

    @Test
    void createReservationDecrementsAvailabilityForAllItems() throws Exception {
        setAvailability("room_types", "available_rooms", "ROOM-101", 5);
        setAvailability("flights", "available_seats", "FL-1001", 18);
        setAvailability("cars", "available_vehicles", "CAR-1001", 8);
        long holdsBefore = count("reservation_availability");

        mockMvc.perform(post("/reservation/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FULL_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        assertThat(availability("room_types", "available_rooms", "ROOM-101")).isEqualTo(4);
        assertThat(availability("flights", "available_seats", "FL-1001")).isEqualTo(17);
        assertThat(availability("cars", "available_vehicles", "CAR-1001")).isEqualTo(7);
        assertThat(count("reservation_availability")).isEqualTo(holdsBefore + 3);
    }

    @Test
    void createFailsWithoutLeavingTracesWhenAvailabilityInsufficient() throws Exception {
        setAvailability("room_types", "available_rooms", "ROOM-101", 0);
        long reservationsBefore = count("reservations");
        long holdsBefore = count("reservation_availability");

        mockMvc.perform(post("/reservation/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(HOTEL_BODY))
                .andExpect(status().isConflict());

        // atomar: weder Reservation noch Holds duerfen persistiert sein
        assertThat(count("reservations")).isEqualTo(reservationsBefore);
        assertThat(count("reservation_availability")).isEqualTo(holdsBefore);
        assertThat(availability("room_types", "available_rooms", "ROOM-101")).isZero();
    }

    @Test
    void expiryRestoresAvailabilityExactlyOnce() throws Exception {
        setAvailability("room_types", "available_rooms", "ROOM-101", 5);
        String number = createHotelReservation();
        assertThat(availability("room_types", "available_rooms", "ROOM-101")).isEqualTo(4);

        // 30 Minuten abgelaufen -> Lazy-Expiry beim naechsten Zugriff.
        // created_at mitverschieben, sonst verletzt das Update den
        // Constraint chk_reservations_expiry (expires_at > created_at).
        jdbcTemplate.update("""
                update reservations
                set created_at = now() - interval '31 minutes',
                    expires_at = now() - interval '1 minute'
                where reservation_number = ?
                """, number);

        mockMvc.perform(get("/reservation/snapshot").param("reservationNumber", number))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));
        assertThat(availability("room_types", "available_rooms", "ROOM-101")).isEqualTo(5);
        assertThat(restoredHolds(number)).isEqualTo(1);

        // zweiter Zugriff auf die bereits EXPIRED-Reservation: keine doppelte Rueckgabe
        mockMvc.perform(get("/reservation/snapshot").param("reservationNumber", number))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));
        assertThat(availability("room_types", "available_rooms", "ROOM-101")).isEqualTo(5);
        assertThat(restoredHolds(number)).isEqualTo(1);
    }

    @Test
    void cancelRestoresAvailabilityExactlyOnce() throws Exception {
        setAvailability("room_types", "available_rooms", "ROOM-101", 5);
        String number = createHotelReservation();
        assertThat(availability("room_types", "available_rooms", "ROOM-101")).isEqualTo(4);

        mockMvc.perform(post("/reservation/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationNumber\":\"" + number + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(availability("room_types", "available_rooms", "ROOM-101")).isEqualTo(5);
        assertThat(restoredHolds(number)).isEqualTo(1);

        // erneuter Cancel laeuft ins Leere: 409, keine weitere Rueckgabe
        mockMvc.perform(post("/reservation/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reservationNumber\":\"" + number + "\"}"))
                .andExpect(status().isConflict());
        assertThat(availability("room_types", "available_rooms", "ROOM-101")).isEqualTo(5);
        assertThat(restoredHolds(number)).isEqualTo(1);
    }

    @Test
    void pendingNotExpiredReservationKeepsAvailabilityReserved() throws Exception {
        setAvailability("room_types", "available_rooms", "ROOM-101", 5);
        String number = createHotelReservation();

        // Solange die Reservation PENDING und gueltig ist (bzw. spaeter bezahlt
        // wurde), bleibt die Availability reserviert - keine Rueckgabe.
        mockMvc.perform(get("/reservation/snapshot").param("reservationNumber", number))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        assertThat(availability("room_types", "available_rooms", "ROOM-101")).isEqualTo(4);
        assertThat(restoredHolds(number)).isZero();
    }

    @Test
    void concurrentReservationsCannotOversellLastSeat() throws Exception {
        setAvailability("flights", "available_seats", "FL-1001", 1);
        long reservationsBefore = count("reservations");
        long holdsBefore = count("reservation_availability");
        CreateReservationRequest request = CreateReservationRequest.builder()
                .origin("Frankfurt am Main").destination("Barcelona")
                .departureDate(LocalDate.of(2026, 10, 10))
                .adults(1).currency("EUR")
                .flightId("FL-1001")
                .build();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<Integer> first = pool.submit(() -> {
            start.await();
            return tryCreate(request);
        });
        Future<Integer> second = pool.submit(() -> {
            start.await();
            return tryCreate(request);
        });
        start.countDown();
        int statusFirst = first.get();
        int statusSecond = second.get();
        pool.shutdown();

        // genau einer gewinnt, genau einer scheitert an der Availability
        assertThat(List.of(statusFirst, statusSecond))
                .containsExactlyInAnyOrder(HttpStatus.CREATED.value(), HttpStatus.CONFLICT.value());
        assertThat(availability("flights", "available_seats", "FL-1001")).isZero();
        assertThat(count("reservations")).isEqualTo(reservationsBefore + 1);
        assertThat(count("reservation_availability")).isEqualTo(holdsBefore + 1);
    }

    // ---------- helpers ----------

    /*
     * Der Service legt die Reservation an und wirft bei fehlender
     * Availability 409 - der HTTP-Status als Proxy fuer den Controller.
     */
    private int tryCreate(CreateReservationRequest request) {
        try {
            reservationService.create(request);
            return HttpStatus.CREATED.value();
        } catch (ResponseStatusException e) {
            return e.getStatusCode().value();
        }
    }

    private String createHotelReservation() throws Exception {
        MvcResult result = mockMvc.perform(post("/reservation/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(HOTEL_BODY))
                .andExpect(status().isCreated())
                .andReturn();
        return new ObjectMapper().readTree(result.getResponse().getContentAsString())
                .path("reservationNumber").asText();
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
    }

    private long restoredHolds(String reservationNumber) {
        return jdbcTemplate.queryForObject("""
                        select count(*)
                        from reservation_availability a
                        join reservation_items i on i.id = a.reservation_item_id
                        join reservations r on r.id = i.reservation_id
                        where r.reservation_number = ? and a.restored
                        """, Long.class, reservationNumber);
    }

    private void setAvailability(String table, String column, String externalId, int value) {
        jdbcTemplate.update("update " + table + " set " + column + " = ? where external_id = ?",
                value, externalId);
    }

    private int availability(String table, String column, String externalId) {
        return jdbcTemplate.queryForObject(
                "select " + column + " from " + table + " where external_id = ?",
                Integer.class, externalId);
    }
}

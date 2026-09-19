package com.coe.b04.server.seeder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Integration tests fuer den CatalogSeeder (Phase 1) gegen ein echtes
 * PostgreSQL im Testcontainer (Schema: db/catalog.sql).
 *
 * Covered:
 *  - leere DB + Application Start -> Seeder -> alle JSON-Daten vorhanden
 *    (inkl. erwarteter Anzahl je Tabelle und erhaltener externer Mock-IDs)
 *  - zweiter Seeder-Lauf auf befuellter DB -> bestehende Daten bleiben
 *    unveraendert (kein Ueberschreiben, keine Duplikate)
 *  - POST /admin/catalog/reset -> Katalogdaten werden geloescht und neu
 *    aus den JSON-Dateien eingespielt
 *
 * Erwartete Anzahl je Tabelle wird aus data/*.json berechnet, nicht
 * hartkodiert - die Tests bleiben bei Datenpflege gruen.
 *
 * Laeuft NICHT im CI (GitHub Actions setzt CI=true): die Testdaten liegen
 * dort nicht als data/*.json vor; die Container-Tests werden nur lokal
 * ausgefuehrt.
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class CatalogSeederIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CatalogSeeder catalogSeeder;

    @Test
    void seedsEmptyDatabaseWithAllMockData() throws Exception {
        int[] expected = expectedCounts();

        assertThat(count("airports")).isEqualTo(expected[0]);
        assertThat(count("flights")).isEqualTo(expected[1]);
        assertThat(count("hotels")).isEqualTo(expected[2]);
        assertThat(count("room_types")).isEqualTo(expected[3]);
        assertThat(count("car_providers")).isEqualTo(expected[4]);
        assertThat(count("cars")).isEqualTo(expected[5]);
        assertThat(count("car_locations")).isEqualTo(expected[6]);
        assertThat(count("car_extras")).isEqualTo(expected[7]);

        // externe Mock-IDs sind erhalten und eindeutig
        assertThat(externalIds("hotels")).contains("HOT-1001").doesNotHaveDuplicates();
        assertThat(externalIds("flights")).contains("FL-1001", "FL-4758").doesNotHaveDuplicates();
        assertThat(externalIds("room_types")).contains("ROOM-101").doesNotHaveDuplicates();
        assertThat(externalIds("car_providers")).contains("PROV-SIXT-01").doesNotHaveDuplicates();
        assertThat(externalIds("cars")).contains("CAR-1001", "CAR-1028").doesNotHaveDuplicates();
        assertThat(externalIds("car_locations")).contains("LOC-BCN-AP").doesNotHaveDuplicates();
        assertThat(externalIds("airports")).contains("BCN").doesNotHaveDuplicates();
    }

    @Test
    void secondSeedKeepsExistingDataUntouched() {
        jdbcTemplate.update("update hotels set name = 'MUTATED' where external_id = 'HOT-1001'");
        long hotelsBefore = count("hotels");

        SeedResult result = catalogSeeder.seedIfEmpty();

        assertThat(result.anythingSeeded()).isFalse();
        assertThat(count("hotels")).isEqualTo(hotelsBefore);
        assertThat(jdbcTemplate.queryForObject(
                "select name from hotels where external_id = 'HOT-1001'", String.class))
                .isEqualTo("MUTATED");
    }

    @Test
    void adminResetReseedsCatalogFromJson() throws Exception {
        int[] expected = expectedCounts();
        jdbcTemplate.update("update hotels set name = 'MUTATED' where external_id = 'HOT-1001'");

        mockMvc.perform(post("/admin/catalog/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.airports").value(expected[0]))
                .andExpect(jsonPath("$.flights").value(expected[1]))
                .andExpect(jsonPath("$.hotels").value(expected[2]))
                .andExpect(jsonPath("$.rooms").value(expected[3]))
                .andExpect(jsonPath("$.providers").value(expected[4]))
                .andExpect(jsonPath("$.cars").value(expected[5]))
                .andExpect(jsonPath("$.locations").value(expected[6]))
                .andExpect(jsonPath("$.extras").value(expected[7]));

        assertThat(jdbcTemplate.queryForObject(
                "select name from hotels where external_id = 'HOT-1001'", String.class))
                .isEqualTo("Hotel Barcelona Center");
        assertThat(count("hotels")).isEqualTo(expected[2]);
    }

    // ---------- helpers ----------

    private long count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
    }

    private List<String> externalIds(String table) {
        return jdbcTemplate.queryForList("select external_id from " + table, String.class);
    }

    /*
     * Erwartete Anzahl je Tabelle aus den JSON-Dateien: {airports, flights,
     * hotels, rooms, providers, cars, locations, extras}.
     */
    private int[] expectedCounts() throws Exception {
        JsonNode airports = read("airports.json");
        JsonNode flights = read("flights.json");
        JsonNode hotels = read("hotels.json");
        JsonNode providers = read("cars.json");

        int rooms = 0;
        for (JsonNode hotel : hotels) {
            rooms += hotel.path("roomTypes").size();
        }
        int cars = 0;
        int extras = 0;
        Set<String> locations = new HashSet<>();
        for (JsonNode provider : providers) {
            for (JsonNode car : provider.path("cars")) {
                cars++;
                locations.add(car.path("locations").path("pickupLocation").path("locationId").asText());
                locations.add(car.path("locations").path("returnLocation").path("locationId").asText());
                extras += car.path("additionalExtras").size();
            }
        }
        return new int[]{airports.size(), flights.size(), hotels.size(), rooms,
                providers.size(), cars, locations.size(), extras};
    }

    private JsonNode read(String file) throws Exception {
        try (InputStream is = Files.newInputStream(Path.of("data", file))) {
            return new ObjectMapper().readTree(is);
        }
    }
}

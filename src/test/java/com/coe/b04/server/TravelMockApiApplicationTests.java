package com.coe.b04.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * Integration tests for the whole application.
 *
 * Unlike the unit tests in this project (which test a single layer in isolation
 * with mocked dependencies), these tests boot the REAL Spring context via
 * @SpringBootTest and send real HTTP requests through MockMvc:
 *
 *   HTTP request -> Controller -> Service -> Repository -> JSON response
 *
 * The real remote profile is active, so the mock data is loaded from the
 * Supabase S3 bucket (credentials via .env or SUPABASE_* environment
 * variables), and the search results asserted below come from that data.
 *
 * Covered end-to-end:
 *  - application context starts and mock data loads (contextLoads)
 *  - hotel search returns only hotels of the requested city
 *  - hotel search reduces roomTypes to rooms matching the max price
 *  - car search returns providers whose cars all match the pickup location
 *  - flight search finds flights for a route and departure date
 *  - request validation returns 400 for missing required parameters
 *
 * Note: these tests are slower than the unit tests (~seconds) and require the
 * Supabase S3 credentials (.env or environment variables) plus network access.
 */

@SpringBootTest(classes = TravelMockRemoteApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("remote")
class TravelMockApiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void hotelSearchReturnsOnlyHotelsOfRequestedCity() throws Exception {
        mockMvc.perform(get("/api/hotels/search")
                        .param("destination", "Barcelona")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalCount").value(greaterThan(0)))
                .andExpect(jsonPath("$.hotels[*].city", everyItem(is("Barcelona"))));
    }

    @Test
    void hotelSearchReducesRoomsByMaxPrice() throws Exception {
        mockMvc.perform(get("/api/hotels/search")
                        .param("destination", "Barcelona")
                        .param("maxPrice", "100")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(greaterThan(0)))
                .andExpect(jsonPath("$.hotels[*].roomTypes[*].pricePerNight",
                        everyItem(lessThanOrEqualTo(100.0))));
    }

    @Test
    void carSearchReturnsProvidersWithMatchingCars() throws Exception {
        mockMvc.perform(get("/api/cars/search")
                        .param("location", "Barcelona")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(greaterThan(0)))
                .andExpect(jsonPath("$.providers[*].cars[*].locations.pickupLocation.city",
                        everyItem(is("Barcelona"))));
    }

    @Test
    void flightSearchFindsFlightsForRouteAndDate() throws Exception {
        mockMvc.perform(get("/api/flights/search")
                        .param("origin", "Düsseldorf")
                        .param("destination", "Barcelona")
                        .param("departureDate", "2026-10-10")
                        .param("numberOfAdults", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(greaterThan(0)))
                .andExpect(jsonPath("$.flights[0].departureTime", startsWith("2026-10-10")));
    }

    @Test
    void hotelSearchRequiresDestination() throws Exception {
        mockMvc.perform(get("/api/hotels/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void carSearchRequiresLocation() throws Exception {
        mockMvc.perform(get("/api/cars/search"))
                .andExpect(status().isBadRequest());
    }
}

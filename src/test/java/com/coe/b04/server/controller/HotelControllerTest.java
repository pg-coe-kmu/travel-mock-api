package com.coe.b04.server.controller;

import com.coe.b04.server.io.HotelRequest;
import com.coe.b04.server.io.HotelResponse;
import com.coe.b04.server.service.HotelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HotelControllerTest {

    private MockMvc mockMvc;
    private HotelService hotelService;

    @BeforeEach
    void setUp() {
        hotelService = mock(HotelService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new HotelController(hotelService)).build();
    }

    @Test
    void searchHotelsBindsRequestParams() throws Exception {
        when(hotelService.search(any())).thenReturn(new HotelResponse(List.of()));

        mockMvc.perform(get("/api/hotels/search")
                        .param("destination", "Barcelona")
                        .param("stars", "4")
                        .param("roomType", "Standard Double")
                        .param("maxPrice", "150")
                        .param("board", "Breakfast")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<HotelRequest> captor = ArgumentCaptor.forClass(HotelRequest.class);
        verify(hotelService).search(captor.capture());
        HotelRequest request = captor.getValue();
        assertEquals("Barcelona", request.getDestination());
        assertEquals(4, request.getStars());
        assertEquals("Standard Double", request.getRoomType());
        assertEquals(150.0, request.getMaxPrice());
        assertEquals("Breakfast", request.getBoard());
    }

    @Test
    void shouldReturnBadRequestWhenDestinationMissing() throws Exception {
        mockMvc.perform(get("/api/hotels/search")
                        .param("stars", "4"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(hotelService);
    }

    @Test
    void shouldUseDefaultsForOptionalParams() throws Exception {
        when(hotelService.search(any())).thenReturn(new HotelResponse(List.of()));

        mockMvc.perform(get("/api/hotels/search")
                        .param("destination", "Barcelona"))
                .andExpect(status().isOk());

        ArgumentCaptor<HotelRequest> captor = ArgumentCaptor.forClass(HotelRequest.class);
        verify(hotelService).search(captor.capture());
        HotelRequest request = captor.getValue();
        assertEquals(0, request.getNumberOfAdults());
        assertEquals(0, request.getNumberOfChildren());
        assertNull(request.getStars());
        assertNull(request.getRoomType());
        assertNull(request.getMaxPrice());
    }

    @Test
    void bindsBooleanAndPriceFilters() throws Exception {
        when(hotelService.search(any())).thenReturn(new HotelResponse(List.of()));

        mockMvc.perform(get("/api/hotels/search")
                        .param("destination", "Barcelona")
                        .param("freeCancellation", "true")
                        .param("minPrice", "50")
                        .param("bedType", "1 King Bed"))
                .andExpect(status().isOk());

        ArgumentCaptor<HotelRequest> captor = ArgumentCaptor.forClass(HotelRequest.class);
        verify(hotelService).search(captor.capture());
        HotelRequest request = captor.getValue();
        assertTrue(request.getFreeCancellation());
        assertEquals(50.0, request.getMinPrice());
        assertEquals("1 King Bed", request.getBedType());
    }

    @Test
    void bindsAmenitiesAsList() throws Exception {
        when(hotelService.search(any())).thenReturn(new HotelResponse(List.of()));

        mockMvc.perform(get("/api/hotels/search")
                        .param("destination", "Barcelona")
                        .param("hotelAmenities", "Free WiFi")
                        .param("hotelAmenities", "Spa"))
                .andExpect(status().isOk());

        ArgumentCaptor<HotelRequest> captor = ArgumentCaptor.forClass(HotelRequest.class);
        verify(hotelService).search(captor.capture());
        assertEquals(List.of("Free WiFi", "Spa"), captor.getValue().getHotelAmenities());
    }

    @Test
    void shouldReturnBadRequestForNegativeMinRating() throws Exception {
        mockMvc.perform(get("/api/hotels/search")
                        .param("destination", "Barcelona")
                        .param("minRating", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(hotelService);
    }

    @Test
    void shouldReturnBadRequestForNegativeGuests() throws Exception {
        mockMvc.perform(get("/api/hotels/search")
                        .param("destination", "Barcelona")
                        .param("numberOfAdults", "-1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(hotelService);
    }

    @Test
    void returnsJsonResponseWithCount() throws Exception {
        when(hotelService.search(any())).thenReturn(new HotelResponse(List.of()));

        mockMvc.perform(get("/api/hotels/search")
                        .param("destination", "Barcelona")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.hotels").isEmpty());
    }
}

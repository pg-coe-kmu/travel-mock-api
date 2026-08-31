package com.coe.b04.server.service;

import com.coe.b04.server.io.HotelRequest;
import com.coe.b04.server.io.HotelResponse;
import com.coe.b04.server.model.Hotel;
import com.coe.b04.server.repository.HotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HotelServiceTest {

    private HotelService hotelService;
    private HotelRepository hotelRepository;

    @BeforeEach
    void setUp() {
        hotelRepository = mock(HotelRepository.class);
        hotelService = new HotelService(hotelRepository);
    }

    @Test
    void searchDelegatesToRepositoryAndWrapsResponse() {
        HotelRequest request = HotelRequest.builder().destination("Barcelona").build();
        Hotel hotel = Hotel.builder().hotelId("HOT-1").city("Barcelona").build();
        when(hotelRepository.findByCityAndOptionals(request)).thenReturn(List.of(hotel));

        HotelResponse response = hotelService.search(request);

        assertEquals(1, response.getTotalCount());
        assertEquals(List.of(hotel), response.getHotels());
        assertNotNull(response.getTimestamp());
        verify(hotelRepository).findByCityAndOptionals(request);
    }

    @Test
    void searchHandlesNullResult() {
        HotelRequest request = HotelRequest.builder().destination("Barcelona").build();
        when(hotelRepository.findByCityAndOptionals(request)).thenReturn(null);

        HotelResponse response = hotelService.search(request);

        assertEquals(0, response.getTotalCount());
        assertNull(response.getHotels());
    }
}

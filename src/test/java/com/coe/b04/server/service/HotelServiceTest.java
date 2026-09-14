package com.coe.b04.server.service;

import com.coe.b04.server.io.HotelRequest;
import com.coe.b04.server.io.HotelResponse;
import com.coe.b04.server.model.Hotel;
import com.coe.b04.server.model.RoomType;
import com.coe.b04.server.repository.HotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Test
    void getDetailsReturnsFilteredHotel() {
        RoomType room = new RoomType();
        room.setRoomId("ROOM-1");
        Hotel hotel = Hotel.builder()
                .hotelId("HOT-1")
                .roomTypes(List.of(room))
                .build();
        when(hotelRepository.findByHotelIdAndRoomId("HOT-1", "ROOM-1")).thenReturn(hotel);

        Hotel result = hotelService.getDetails("HOT-1", "ROOM-1");

        assertEquals(hotel, result);
        verify(hotelRepository).findByHotelIdAndRoomId("HOT-1", "ROOM-1");
    }

    @Test
    void getDetailsThrowsNotFound() {
        when(hotelRepository.findByHotelIdAndRoomId("HOT-1", "ROOM-UNKNOWN")).thenReturn(null);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> hotelService.getDetails("HOT-1", "ROOM-UNKNOWN"));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void getDetailsReturnsFullHotelWhenRoomIdNull() {
        Hotel hotel = Hotel.builder().hotelId("HOT-1").build();
        when(hotelRepository.findById("HOT-1")).thenReturn(hotel);

        Hotel result = hotelService.getDetails("HOT-1", null);

        assertEquals(hotel, result);
        verify(hotelRepository).findById("HOT-1");
        verify(hotelRepository, never()).findByHotelIdAndRoomId(anyString(), anyString());
    }

    @Test
    void getDetailsThrowsNotFoundForUnknownHotel() {
        when(hotelRepository.findById("HOT-UNKNOWN")).thenReturn(null);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> hotelService.getDetails("HOT-UNKNOWN", null));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }
}

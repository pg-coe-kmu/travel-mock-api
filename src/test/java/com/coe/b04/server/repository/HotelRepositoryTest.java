package com.coe.b04.server.repository;

import com.coe.b04.server.io.HotelRequest;
import com.coe.b04.server.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotelRepositoryTest {

    private HotelRepository repository;
    private Hotel barcelonaHotel;
    private Hotel madridHotel;

    @BeforeEach
    void setUp() {
        repository = new HotelRepository();

        barcelonaHotel = Hotel.builder()
                .hotelId("HOT-1")
                .name("Hotel Barcelona Center")
                .city("Barcelona")
                .country("Spain")
                .stars(4)
                .rating(new Rating(BigDecimal.valueOf(4.5), 1000))
                .hotelAmenities(List.of("Free WiFi", "Swimming Pool"))
                .roomTypes(List.of(
                        room("ROOM-1", "Standard Double", "Breakfast", "136.00", 2, 1, true),
                        room("ROOM-2", "Standard Double Garden View", "Half Board", "150.00", 2, 0, false)))
                .build();

        madridHotel = Hotel.builder()
                .hotelId("HOT-2")
                .name("Hotel Madrid Royal")
                .city("Madrid")
                .country("Spain")
                .stars(5)
                .rating(new Rating(BigDecimal.valueOf(4.8), 2000))
                .hotelAmenities(List.of("Free WiFi"))
                .roomTypes(List.of(
                        room("ROOM-3", "Deluxe Suite", "All Inclusive", "290.00", 3, 2, true)))
                .build();

        repository.setHotels(List.of(barcelonaHotel, madridHotel));
    }

    private RoomType room(String roomId, String roomType, String board, String pricePerNight,
                          int adults, int children, boolean freeCancellation) {
        RoomType room = new RoomType();
        room.setRoomId(roomId);
        room.setRoomType(roomType);
        room.setBoard(board);
        room.setPricePerNight(new BigDecimal(pricePerNight));
        room.setMaxOccupancy(new MaxOccupancy(adults, children));
        room.setCancellationPolicy(new CancellationPolicy(freeCancellation, null));
        return room;
    }

    private List<String> roomTypeNames(Hotel hotel) {
        return hotel.getRoomTypes().stream().map(RoomType::getRoomType).toList();
    }

    @Test
    void filtersByCityCaseInsensitive() {
        List<Hotel> result = repository.findByCityAndOptionals(
                HotelRequest.builder().destination("barcelona").build());

        assertEquals(1, result.size());
        assertEquals("HOT-1", result.getFirst().getHotelId());
    }

    @Test
    void filtersByStars() {
        List<Hotel> result = repository.findByCityAndOptionals(
                HotelRequest.builder().destination("Madrid").stars(5).build());

        assertEquals(1, result.size());
        assertEquals("HOT-2", result.getFirst().getHotelId());
    }

    @Test
    void filtersByMinRating() {
        List<Hotel> result = repository.findByCityAndOptionals(
                HotelRequest.builder().destination("Barcelona").minRating(4.6).build());

        assertTrue(result.isEmpty());
    }

    @Test
    void filtersByHotelAmenitiesAllRequired() {
        List<Hotel> result = repository.findByCityAndOptionals(
                HotelRequest.builder().destination("Barcelona")
                        .hotelAmenities(List.of("Free WiFi", "Swimming Pool"))
                        .build());

        assertEquals(1, result.size());
        assertEquals("HOT-1", result.getFirst().getHotelId());
    }

    @Test
    void excludesHotelWhenAmenityIsMissing() {
        List<Hotel> result = repository.findByCityAndOptionals(
                HotelRequest.builder().destination("Madrid")
                        .hotelAmenities(List.of("Free WiFi", "Spa"))
                        .build());

        assertTrue(result.isEmpty());
    }

    @Test
    void roomTypeFilterMatchesAsSubstring() {
        List<Hotel> result = repository.findByCityAndOptionals(
                HotelRequest.builder().destination("Barcelona").roomType("Standard Double").build());

        assertEquals(1, result.size());
        assertEquals(List.of("Standard Double", "Standard Double Garden View"), roomTypeNames(result.getFirst()));
    }

    @Test
    void reducesRoomTypesToMatchingRooms() {
        List<Hotel> result = repository.findByCityAndOptionals(
                HotelRequest.builder().destination("Barcelona").maxPrice(140.0).build());

        assertEquals(1, result.size());
        assertEquals(List.of("Standard Double"), roomTypeNames(result.getFirst()));
    }

    @Test
    void filtersByFreeCancellation() {
        List<Hotel> result = repository.findByCityAndOptionals(
                HotelRequest.builder().destination("Barcelona").freeCancellation(false).build());

        assertEquals(1, result.size());
        assertEquals(List.of("Standard Double Garden View"), roomTypeNames(result.getFirst()));
    }

    @Test
    void filtersByOccupancy() {
        List<Hotel> result = repository.findByCityAndOptionals(
                HotelRequest.builder().destination("Barcelona")
                        .numberOfAdults(3)
                        .build());

        assertTrue(result.isEmpty());
    }

    @Test
    void excludesHotelWithoutMatchingRooms() {
        List<Hotel> result = repository.findByCityAndOptionals(
                HotelRequest.builder().destination("Barcelona").roomType("Penthouse").build());

        assertTrue(result.isEmpty());
    }

    @Test
    void doesNotMutateSharedData() {
        repository.findByCityAndOptionals(
                HotelRequest.builder().destination("Barcelona").maxPrice(140.0).build());

        assertEquals(2, barcelonaHotel.getRoomTypes().size());
        assertEquals(2, repository.getHotels().getFirst().getRoomTypes().size());
    }
}

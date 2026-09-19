package com.coe.b04.server.repository;

import com.coe.b04.server.io.HotelRequest;
import com.coe.b04.server.model.Hotel;
import com.coe.b04.server.model.RoomType;
import org.springframework.stereotype.Repository;

import java.util.List;

/*
 * Katalogdaten kommen ausschliesslich aus PostgreSQL (CatalogQueryRepository)
 * - inklusive aktueller Availability. Die Filterlogik laeuft in Java ueber
 * die DB-geladenen Listen.
 */
@Repository
public class HotelRepository {

    private final CatalogQueryRepository catalogQueryRepository;

    public HotelRepository(CatalogQueryRepository catalogQueryRepository) {
        this.catalogQueryRepository = catalogQueryRepository;
    }

    public List<Hotel> findAll() {
        return catalogQueryRepository.findAllHotels();
    }

    /*
     * Finds the hotel by hotelId. Returns null if the hotel does not exist.
     */
    public Hotel findById(String hotelId) {
        return catalogQueryRepository.findAllHotels().stream()
                .filter(hotel -> hotel.getHotelId().equalsIgnoreCase(hotelId))
                .findFirst()
                .orElse(null);
    }

    /*
     * Finds the hotel by hotelId and returns a copy containing only the room with the given roomId.
     * Returns null if the hotel does not exist or does not contain a room with the given roomId.
     */
    public Hotel findByHotelIdAndRoomId(String hotelId, String roomId) {
        return catalogQueryRepository.findAllHotels().stream()
                .filter(hotel -> hotel.getHotelId().equalsIgnoreCase(hotelId))
                .findFirst()
                .map(hotel -> hotel.toBuilder()
                        .roomTypes(hotel.getRoomTypes().stream()
                                .filter(room -> room.getRoomId().equalsIgnoreCase(roomId))
                                .toList())
                        .build())
                .filter(hotel -> !hotel.getRoomTypes().isEmpty())
                .orElse(null);
    }

    /*
     * Finds hotels by destination and optional filter parameters from the request:
     * stars, minRating, hotelAmenities (hotel level) and
     * roomType, board, bedType, roomAmenities, guests, price range, freeCancellation (room level).
     * The roomTypes of each matched hotel are reduced to the rooms matching all given room filters.
     * Null/empty optional parameters are ignored.
     */
    public List<Hotel> findByCityAndOptionals(HotelRequest request) {
        return catalogQueryRepository.findAllHotels().stream()
                .filter(hotel -> hotel.getCity().equalsIgnoreCase(request.getDestination()))
                .filter(hotel -> request.getStars() == null || hotel.getStars() == request.getStars())
                .filter(hotel -> request.getMinRating() == null
                        || (hotel.getRating() != null && hotel.getRating().getScore().doubleValue() >= request.getMinRating()))
                .filter(hotel -> hasAllAmenities(hotel.getHotelAmenities(), request.getHotelAmenities()))
                .map(hotel -> withMatchingRooms(hotel, request))
                .filter(hotel -> !hotel.getRoomTypes().isEmpty())
                .toList();
    }

    /*
     * Returns a copy of the hotel containing only the rooms that match all given room filters.
     * The original hotel (shared in-memory state) is left untouched.
     */
    private Hotel withMatchingRooms(Hotel hotel, HotelRequest request) {
        List<RoomType> matchingRooms = hotel.getRoomTypes().stream()
                .filter(room -> matchesRoomFilters(room, request))
                .toList();
        return hotel.toBuilder().roomTypes(matchingRooms).build();
    }

    /*
     * Checks if a room matches all given room filters.
     */
    private boolean matchesRoomFilters(RoomType room, HotelRequest request) {
        return (request.getRoomType() == null
                || room.getRoomType().toLowerCase().contains(request.getRoomType().toLowerCase()))
                && (request.getBoard() == null || room.getBoard().equalsIgnoreCase(request.getBoard()))
                && (request.getBedType() == null || room.getBedType().equalsIgnoreCase(request.getBedType()))
                && hasAllAmenities(room.getRoomAmenities(), request.getRoomAmenities())
                && room.getMaxOccupancy().getAdults() >= request.getNumberOfAdults()
                && room.getMaxOccupancy().getChildren() >= request.getNumberOfChildren()
                && (request.getMinPrice() == null || room.getPricePerNight().doubleValue() >= request.getMinPrice())
                && (request.getMaxPrice() == null || room.getPricePerNight().doubleValue() <= request.getMaxPrice())
                && (request.getFreeCancellation() == null
                || (room.getCancellationPolicy() != null
                && room.getCancellationPolicy().isFreeCancellation() == request.getFreeCancellation()));
    }

    /*
     * Checks if the available amenities contain all requested amenities (case-insensitive).
     */
    private boolean hasAllAmenities(List<String> available, List<String> requested) {
        return requested == null || requested.isEmpty()
                || (available != null && requested.stream().allMatch(r ->
                available.stream().anyMatch(a -> a.equalsIgnoreCase(r))));
    }
}

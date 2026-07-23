package com.coe.b04.server.repository;

import com.coe.b04.server.model.Hotel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

import java.util.List;

@Setter
@Getter
@Repository
public class HotelRepository {

    private List<Hotel> hotels;

    public List<Hotel> findAll() {
        return hotels;
    }

    /*
        * Find hotels by city and optional parameters: stars, roomType, maxPrice, and board.
        * If an optional parameter is null, it will be ignored in the filtering process.
        * @param city the city to filter hotels by
        * @param stars the number of stars to filter hotels by (optional)
        * @param roomType the room type to filter hotels by (optional)
        * @param maxPrice the maximum price per night to filter hotels by (optional)
        * @param board the board type to filter hotels by (optional)
        * @return a list of hotels that match the specified criteria
     */
    public List<Hotel> findByCityAndOptionals(String city, Integer stars, String roomType, Double maxPrice, String board) {
        return hotels.stream()
                .filter(hotel -> hotel.getCity().equalsIgnoreCase(city))
                .filter(hotel -> stars == null || hotel.getStars() == stars)
                .filter(hotel -> roomType == null || hotel.getRoomType().equalsIgnoreCase(roomType))
                .filter(hotel -> maxPrice == null || hotel.getPricePerNight().doubleValue() <= maxPrice)
                .filter(hotel -> board == null || hotel.getBoard().equalsIgnoreCase(board))
                .toList();
    }
}

package com.coe.b04.server.service;

import com.coe.b04.server.model.Hotel;
import com.coe.b04.server.repository.HotelRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HotelService {

    private final HotelRepository hotelRepository;

    public HotelService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    public List<Hotel> getHotels() {
        return hotelRepository.findAll();
    }

    public List<Hotel> search(String city, Integer stars, String roomType, Double maxPrice, String board) {
        return hotelRepository.findByCityAndOptionals(city, stars, roomType, maxPrice, board);
    }
}

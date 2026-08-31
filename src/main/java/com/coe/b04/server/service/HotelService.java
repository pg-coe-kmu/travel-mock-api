package com.coe.b04.server.service;

import com.coe.b04.server.io.HotelRequest;
import com.coe.b04.server.io.HotelResponse;
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

    public HotelResponse search(HotelRequest hotelRequest) {
        List<Hotel> hotels = hotelRepository.findByCityAndOptionals(hotelRequest);
        return new HotelResponse(hotels);
    }
}

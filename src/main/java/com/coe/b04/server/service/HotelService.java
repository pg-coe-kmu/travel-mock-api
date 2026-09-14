package com.coe.b04.server.service;

import com.coe.b04.server.io.HotelRequest;
import com.coe.b04.server.io.HotelResponse;
import com.coe.b04.server.model.Hotel;
import com.coe.b04.server.repository.HotelRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public Hotel getDetails(String hotelId, String roomId) {
        if (roomId == null) {
            Hotel hotel = hotelRepository.findById(hotelId);
            if (hotel == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No hotel found for hotelId: " + hotelId);
            }
            return hotel;
        }
        Hotel hotel = hotelRepository.findByHotelIdAndRoomId(hotelId, roomId);
        if (hotel == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No room found for hotelId: " + hotelId + ", roomId: " + roomId);
        }
        return hotel;
    }
}

package com.coe.b04.server.controller;

import com.coe.b04.server.model.Hotel;
import com.coe.b04.server.service.HotelService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @GetMapping("/search")
    public List<Hotel> search(@RequestParam String destination,
                              @RequestParam(required = false) Integer stars,
                              @RequestParam(required = false) String roomType,
                              @RequestParam(required = false) Double maxPrice,
                              @RequestParam(required = false) String board) {
        return hotelService.search(destination, stars, roomType, maxPrice, board);
    }
}

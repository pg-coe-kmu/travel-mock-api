package com.coe.b04.server.controller;

import com.coe.b04.server.io.HotelRequest;
import com.coe.b04.server.io.HotelResponse;
import com.coe.b04.server.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Hotels", description = "Hotel search")
@RestController
@RequestMapping("api/hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @Operation(summary = "Search hotels",
            description = "Searches hotels by destination, star rating, amenities, room preferences, "
                    + "guest counts and optional filters such as price range and free cancellation.")
    @GetMapping("/search")
    public ResponseEntity<HotelResponse> search(@Valid @ModelAttribute HotelRequest hotelRequest) {
        HotelResponse response = hotelService.search(hotelRequest);
        return ResponseEntity.ok(response);
    }
}

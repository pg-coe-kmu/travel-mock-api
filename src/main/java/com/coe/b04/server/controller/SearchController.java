package com.coe.b04.server.controller;

import com.coe.b04.server.io.CarRequest;
import com.coe.b04.server.io.CarResponse;
import com.coe.b04.server.io.FlightRequest;
import com.coe.b04.server.io.FlightResponse;
import com.coe.b04.server.io.HotelRequest;
import com.coe.b04.server.io.HotelResponse;
import com.coe.b04.server.service.CarService;
import com.coe.b04.server.service.FlightService;
import com.coe.b04.server.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Search", description = "Search rental car, hotel or flight by various criteria")
@RestController
public class SearchController {

    private final CarService carService;
    private final HotelService hotelService;
    private final FlightService flightService;

    public SearchController(CarService carService, HotelService hotelService, FlightService flightService) {
        this.carService = carService;
        this.hotelService = hotelService;
        this.flightService = flightService;
    }

    @Operation(summary = "Search rental cars",
            description = "Searches rental cars by location, provider, vehicle specifications "
                    + "and optional filters such as price range and free cancellation.")
    @GetMapping("search/cars")
    public ResponseEntity<CarResponse> searchCars(@Valid @ModelAttribute CarRequest carRequest) {
        CarResponse response = carService.search(carRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search hotels",
            description = "Searches hotels by destination, star rating, amenities, room preferences, "
                    + "guest counts and optional filters such as price range and free cancellation.")
    @GetMapping("search/hotels")
    public ResponseEntity<HotelResponse> searchHotels(@Valid @ModelAttribute HotelRequest hotelRequest) {
        HotelResponse response = hotelService.search(hotelRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search flights",
            description = "Searches flights by origin, destination, travel dates, passenger counts "
                    + "and optional filters such as travel class and maximum price.")
    @GetMapping("search/flights")
    public ResponseEntity<FlightResponse> searchFlights(@Valid @ModelAttribute FlightRequest flightRequest) {
        FlightResponse response = flightService.search(flightRequest);
        return ResponseEntity.ok(response);
    }
}

package com.coe.b04.server.controller;

import com.coe.b04.server.io.FlightRequest;
import com.coe.b04.server.io.FlightResponse;
import com.coe.b04.server.service.FlightService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @GetMapping("/search")
    public ResponseEntity<FlightResponse> searchFlights(@Valid @ModelAttribute FlightRequest flightRequest) {
        FlightResponse response = flightService.search(flightRequest);
        return ResponseEntity.ok(response);
    }
}

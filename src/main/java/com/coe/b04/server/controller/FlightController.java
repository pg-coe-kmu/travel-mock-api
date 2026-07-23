package com.coe.b04.server.controller;

import com.coe.b04.server.model.Flight;
import com.coe.b04.server.service.FlightService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("api/flights")
public class FlightController {
    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }


    @GetMapping("/search")
    public List<Flight> search(@RequestParam String from,
                               @RequestParam String destination,
                               @RequestParam LocalDate departureDate,
                               @RequestParam(required = false) String travelClass) {
        return flightService.searchFlights(from, destination, departureDate, travelClass);
    }
}

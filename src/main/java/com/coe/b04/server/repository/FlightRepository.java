package com.coe.b04.server.repository;

import com.coe.b04.server.model.Flight;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

import java.util.List;

@Getter
@Setter
@Repository
public class FlightRepository {
    private List<Flight> flights;
}

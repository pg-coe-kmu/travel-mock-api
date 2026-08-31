package com.coe.b04.server.service;

import com.coe.b04.server.io.CarRequest;
import com.coe.b04.server.io.CarResponse;
import com.coe.b04.server.model.CarProvider;
import com.coe.b04.server.repository.CarRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarService {

    private final CarRepository carRepository;

    public CarService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    public CarResponse search(CarRequest carRequest) {
        List<CarProvider> providers = carRepository.findByLocationAndOptionals(carRequest);
        return new CarResponse(providers);
    }
}

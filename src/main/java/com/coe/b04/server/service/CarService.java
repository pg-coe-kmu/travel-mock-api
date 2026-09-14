package com.coe.b04.server.service;

import com.coe.b04.server.io.CarRequest;
import com.coe.b04.server.io.CarResponse;
import com.coe.b04.server.model.CarProvider;
import com.coe.b04.server.repository.CarRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public CarProvider getDetails(String providerId, String carId) {
        if (carId == null) {
            CarProvider provider = carRepository.findByProviderId(providerId);
            if (provider == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No provider found for providerId: " + providerId);
            }
            return provider;
        }
        CarProvider provider = carRepository.findByProviderIdAndCarId(providerId, carId);
        if (provider == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No car found for providerId: " + providerId + ", carId: " + carId);
        }
        return provider;
    }
}

package com.coe.b04.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Car {
    private String carId;
    private String vehicleClass;
    private String categoryCode;
    private String brand;
    private String model;

    private int availableVehicles;

    private CarLocations locations;

    private CarSpecifications specifications;

    private CarPricing pricing;

    private List<String> includedServices;

    private CancellationPolicy cancellationPolicy;

    private List<AdditionalExtra> additionalExtras;
}

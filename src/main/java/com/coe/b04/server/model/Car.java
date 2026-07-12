package com.coe.b04.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Car {
    private String carId;

    private String provider;

    private String pickupLocation;
    private String returnLocation;

    private String vehicleClass;
    private String brand;

    private String transmission;
    private String fuel;

    private int doors;

    private boolean airCondition;

    private BigDecimal pricePerDay;
    private String currency;

    private int availableVehicles;
}

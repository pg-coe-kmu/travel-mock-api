package com.coe.b04.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarSpecifications {
    private String transmission;
    private String fuelType;

    private int doors;
    private int seats;

    private LuggageCapacity luggageCapacity;

    private boolean airCondition;
    private String driveType;
}

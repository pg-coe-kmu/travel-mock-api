package com.coe.b04.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarLocation {
    private String locationId;
    private String name;
    private String city;
    private String address;
    private String openingHours;
}

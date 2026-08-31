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
public class CarProvider {
    private String providerId;
    private String providerName;

    private Rating rating;

    private String baseCurrency;

    private ProviderPolicies providerPolicies;

    private List<Car> cars;
}

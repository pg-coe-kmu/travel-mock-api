package com.coe.b04.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProviderPolicies {
    private int minDriverAge;
    private BigDecimal youngDriverFeePerDay;
    private BigDecimal depositAmount;
    private List<String> acceptedPaymentMethods;
}

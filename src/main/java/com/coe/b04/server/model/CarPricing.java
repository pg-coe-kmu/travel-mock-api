package com.coe.b04.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarPricing {
    private BigDecimal pricePerDay;
    private BigDecimal totalPrice;
    private int rentalDays;
}

package com.coe.b04.server.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalExtra {
    private String extraId;
    private String name;
    private BigDecimal pricePerDay;
    private String priceType;
}

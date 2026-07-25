package com.coe.b04.server.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Airport {
    private String iata_code;
    private String name;
    private String location;
}

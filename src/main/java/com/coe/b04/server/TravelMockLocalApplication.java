package com.coe.b04.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;


@Profile("local")
@SpringBootApplication
public class TravelMockLocalApplication {
    public static void main(String[] args) {
        SpringApplication.run(TravelMockLocalApplication.class, args);
    }
}

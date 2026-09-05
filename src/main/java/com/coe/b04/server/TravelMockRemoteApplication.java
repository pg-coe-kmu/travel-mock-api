package com.coe.b04.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

@Profile("remote")
@SpringBootApplication
public class TravelMockRemoteApplication {
    public static void main(String[] args) {
        SpringApplication.run(TravelMockRemoteApplication.class, args);
    }
}

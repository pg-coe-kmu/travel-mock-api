package com.coe.b04.server.repository;

import com.coe.b04.server.model.Car;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

import java.util.List;

@Getter
@Setter
@Repository
public class CarRepository {
    List<Car> cars;
}

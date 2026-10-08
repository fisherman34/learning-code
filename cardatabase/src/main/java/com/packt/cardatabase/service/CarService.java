package com.packt.cardatabase.service;

import com.packt.cardatabase.domain.Car;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class CarService {

    @PreAuthorize(("hasRole('USER')"))
    public void updateCar(Car car) {
        // Update car logic
    }

    @PreAuthorize(("hasRole('ADMIN')"))
    public void deleteCar(Car car) {
        // Delete car logic
    }
}

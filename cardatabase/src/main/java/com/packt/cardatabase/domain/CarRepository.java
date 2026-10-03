package com.packt.cardatabase.domain;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CarRepository extends CrudRepository<Car,Long> {
    List<Car> findByBrand(String brand);

    List<Car> findByColor(String color);

    List<Car> findByModelYear(int modelYear);

    List<Car> findByBrandAndModel(String brand, String model);

    List<Car> findByBrandOrColor(String brand, String color);
}

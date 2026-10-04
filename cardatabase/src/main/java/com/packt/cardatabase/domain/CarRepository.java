package com.packt.cardatabase.domain;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource
public interface CarRepository extends CrudRepository<Car,Long> {

    List<Car> findByBrand(@Param("brand") String brand);
    // Fetch cars by color
    List<Car> findByColor(@Param("color") String color);

    List<Car> findByModelYear(@Param("modelYear") int modelYear);

    List<Car> findByBrandAndModel(@Param("brand") String brand, @Param("model") String model);

    List<Car> findByBrandOrColor(@Param("brand") String brand, @Param("color") String color);
}

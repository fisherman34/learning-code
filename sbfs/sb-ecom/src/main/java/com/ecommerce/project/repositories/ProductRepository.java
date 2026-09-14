package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // methodを定義するだけです。実装はSpring Data JPAが自動で行います。
    List<Product> findByCategoryOrderByPriceAsc(Category category);
}

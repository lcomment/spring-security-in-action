package com.ch6.ssia.domain.product.repository;

import com.ch6.ssia.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}

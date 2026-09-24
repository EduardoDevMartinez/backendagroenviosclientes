package com.agroenvios.clientes.secondary.repository;

import com.agroenvios.clientes.secondary.model.ProductUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductUnitRepository extends JpaRepository<ProductUnit, Integer> {

    List<ProductUnit> findByActiveTrue();
}

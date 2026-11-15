package com.galactica.repository;

import com.galactica.entities.Zona;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZonaRepository extends JpaRepository<Zona, Integer> {
    List<Zona> findByActivoTrue();

    List<Zona> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    Page<Zona> findByActivoTrue(Pageable pageable);

    Page<Zona> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
}

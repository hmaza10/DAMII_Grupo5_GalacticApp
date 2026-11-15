package com.galactica.repository;

import com.galactica.entities.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
    List<Categoria> findByActivoTrue();

    List<Categoria> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    Page<Categoria> findByActivoTrue(Pageable pageable);

    Page<Categoria> findByActivoFalse(Pageable pageable);

    Page<Categoria> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
}

package com.galactica.repository;

import com.galactica.entities.Tipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoRepository extends JpaRepository<Tipo, Integer> {
    Optional<Tipo> findByDescripcionIgnoreCase(String descripcion);
}

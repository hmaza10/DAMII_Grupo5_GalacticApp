package com.galactica.repository;

import com.galactica.entities.EstadoMenu;
import com.galactica.entities.MenuSemanal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MenuSemanalRepository extends JpaRepository<MenuSemanal, Integer> {
    Optional<MenuSemanal> findFirstByEstadoAndFechaFinGreaterThanEqualOrderByFechaInicioAsc(EstadoMenu estado, LocalDate hoy);

    boolean existsByEstadoAndFechaInicio(EstadoMenu estado, LocalDate fechaInicio);

    List<MenuSemanal> findByEstado(EstadoMenu estado);

    Page<MenuSemanal> findAllByOrderByFechaInicioDesc(Pageable pageable);

    Page<MenuSemanal> findByEstado(EstadoMenu estado, Pageable pageable);
}

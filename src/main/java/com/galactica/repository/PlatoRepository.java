package com.galactica.repository;

import com.galactica.entities.Plato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlatoRepository extends JpaRepository<Plato, Integer> {
    List<Plato> findByNombreContainingIgnoreCase(String nombre);

    List<Plato> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre);

    List<Plato> findByActivoTrue();

    @Query("SELECT p FROM Plato p WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) AND p.activo = true")
    List<Plato> buscarPorNombre(@Param("texto") String texto);

    List<Plato> findByCategoria_IdCategoriaAndActivoTrue(Integer idCategoria);

    List<Plato> findByDescripcionContainingIgnoreCaseAndActivoTrue(String descripcion);

    Page<Plato> findByActivoTrue(Pageable pageable);

    Page<Plato> findByActivoFalse(Pageable pageable);

    Page<Plato> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    Page<Plato> findByDescripcionContainingIgnoreCase(String descripcion, Pageable pageable);

    Page<Plato> findByCategoria_IdCategoria(Integer idCategoria, Pageable pageable);

}

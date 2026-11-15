package com.galactica.repository;

import com.galactica.entities.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    Optional<Cliente> findByUsuarioIdUsuario(Integer idUsuario);

    List<Cliente> findByActivoTrue();

    List<Cliente> findByTelefonoContainingAndActivoTrue(String telefono);

    List<Cliente> findByZonaIdZonaAndActivoTrue(Integer idZona);

    @Query("""
    SELECT c FROM Cliente c 
    WHERE LOWER(CONCAT(c.usuario.nombres, ' ', c.usuario.apellidos)) LIKE LOWER(CONCAT('%', :texto, '%'))
    AND c.activo = true
    """)
    List<Cliente> buscarPorNombre(@Param("texto") String texto);

    Page<Cliente> findByActivoTrue(Pageable pageable);

    Page<Cliente> findByTelefonoContaining(String telefono, Pageable pageable);

    Page<Cliente> findByZonaIdZona(Integer idZona, Pageable pageable);

    @Query("""
    SELECT c FROM Cliente c
    WHERE LOWER(CONCAT(c.usuario.nombres, ' ', c.usuario.apellidos)) LIKE LOWER(CONCAT('%', 
    :texto, '%'))
    """)
    Page<Cliente> buscarPorNombrePaginado(@Param("texto") String texto, Pageable pageable);
}

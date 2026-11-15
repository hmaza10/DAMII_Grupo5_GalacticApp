package com.galactica.repository;

import com.galactica.entities.EstadoPedido;
import com.galactica.entities.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    List<Pedido> findByClienteIdClienteOrderByFechaCreacionDesc(Integer idCliente);

    Page<Pedido> findByClienteIdCliente(Integer idCliente, Pageable pageable);

    List<Pedido> findByEstado(EstadoPedido estado);

    long countByEstado(EstadoPedido estado);

    @Query("""
    SELECT p FROM Pedido p
    WHERE (:estado IS NULL OR p.estado = :estado) AND (:idCliente IS NULL OR p.cliente.idCliente = :idCliente)
    AND (:idZona IS NULL OR p.zona.idZona = :idZona) AND (:fechaDesde IS NULL OR p.fechaCreacion >= :fechaDesde)
    AND (:fechaHasta IS NULL OR p.fechaCreacion <= :fechaHasta)
    """)
    Page<Pedido> buscarPaginaConFiltros(@Param("estado") EstadoPedido estado,
                                        @Param("idCliente") Integer idCliente,
                                        @Param("idZona") Integer idZona,
                                        @Param("fechaDesde") LocalDateTime fechaDesde,
                                        @Param("fechaHasta") LocalDateTime fechaHasta,
                                        Pageable pageable);
}

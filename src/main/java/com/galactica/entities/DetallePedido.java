package com.galactica.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "tbl_detalle_pedido")
public class DetallePedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @ManyToOne
    @JoinColumn(name = "id_pedido")
    @JsonIgnoreProperties("detalles")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Pedido pedido;

    @Column(name = "fecha_consumo")
    private LocalDate fechaConsumo;

    @Column(name = "dia_semana")
    private String diaSemana;

    @ManyToOne
    @JoinColumn(name = "id_plato")
    private Plato plato;

    @Column(name = "cantidad")
    private Integer cantidad = 1;

    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    @Column(name = "subtotal")
    private BigDecimal subtotal;
}

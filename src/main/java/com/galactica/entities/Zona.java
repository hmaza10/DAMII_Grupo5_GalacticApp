package com.galactica.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "tbl_zona")
public class Zona {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_zona")
    private Integer idZona;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "tarifa_delivery")
    private BigDecimal tarifaDelivery = BigDecimal.ZERO;

    @Column(name = "activo")
    private Boolean activo = true;
}

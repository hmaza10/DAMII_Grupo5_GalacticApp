package com.galactica.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "tbl_plato")
public class Plato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plato")
    private Integer idPlato;

    @ManyToOne
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "precio")
    private BigDecimal precio;

    @Column(name = "imagen_url")
    private String imagenUrl;

    @Column(name = "activo")
    private Boolean activo = true;
}

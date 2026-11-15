package com.galactica.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "tbl_menu_dia")
public class MenuDia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_menu_dia")
    private Integer idMenuDia;

    @ManyToOne
    @JoinColumn(name = "id_menu")
    private MenuSemanal menuSemanal;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "dia_semana")
    private String diaSemana;

    @ManyToOne
    @JoinColumn(name = "id_plato_opcion1")
    private Plato opcion1;

    @ManyToOne
    @JoinColumn(name = "id_plato_opcion2")
    private Plato opcion2;
}

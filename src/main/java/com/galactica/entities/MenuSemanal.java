package com.galactica.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "tbl_menu_semanal")
public class MenuSemanal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_menu")
    private Integer idMenu;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoMenu estadoMenu = EstadoMenu.BORRADOR;

    @Column(name = "publicado_en")
    private LocalDateTime publicadoEn;

    @OneToMany(mappedBy = "menuSemanal", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("menuSemanal")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<MenuDia> dias = new ArrayList<>();
}

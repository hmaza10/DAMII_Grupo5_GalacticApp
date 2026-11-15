package com.galactica.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tbl_cliente")
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @OneToOne
    @JoinColumn(name = "id_usuario", unique = true)
    private Usuario usuario;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "direccion")
    private String direccion;

    @ManyToOne
    @JoinColumn(name = "id_zona")
    private Zona zona;

    @Column(name = "referencia")
    private String referencia;

    @Column(name = "activo")
    private Boolean activo = true;
}

package com.galactica.service;

import com.galactica.entities.Categoria;
import com.galactica.entities.Plato;
import com.galactica.exception.ApiException;
import com.galactica.repository.CategoriaRepository;
import com.galactica.repository.PlatoRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlatoService {

    private final PlatoRepository platoRepository;
    private final CategoriaRepository categoriaRepository;

    public PlatoService(PlatoRepository platoRepository, CategoriaRepository categoriaRepository) {
        this.platoRepository = platoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public List<Plato> listarActivos() {
        return platoRepository.findByActivoTrue();
    }

    public Page<Plato> listarPaginado(int page, int size, String criterio, String texto, Integer idCategoria) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 50)),
                Sort.by("idPlato").ascending());
        boolean hayTexto = texto != null && !texto.isBlank();
        switch (criterio) {
            case "categoria":
                if (idCategoria != null) {
                    return platoRepository.findByCategoria_IdCategoria(idCategoria, pageable);
                }
                break;
            case "descripcion":
                if (hayTexto) {
                    return platoRepository.findByDescripcionContainingIgnoreCase(texto.trim(), pageable);
                }
                break;
            case "nombre":
            default:
                if (hayTexto) {
                    return platoRepository.findByNombreContainingIgnoreCase(texto.trim(), pageable);
                }
                break;
        }
        return platoRepository.findAll(pageable);
    }

    public Plato obtener(Integer id) {
        return platoRepository.findById(id).orElseThrow(() -> ApiException.noEncontrado("El plato no existe"));
    }

    public List<Plato> buscarPorNombre(String texto) {
        return platoRepository.buscarPorNombre(texto);
    }

    public List<Plato> buscarPorCategoria(Integer idCategoria) {
        return platoRepository.findByCategoria_IdCategoriaAndActivoTrue(idCategoria);
    }

    public List<Plato> buscarPorDescripcion(String texto) {
        return platoRepository.findByDescripcionContainingIgnoreCaseAndActivoTrue(texto);
    }

    @Transactional
    public Plato crear(Plato plato) {
        Categoria categoria = validarYResolverCategoria(plato);
        plato.setIdPlato(null);
        plato.setNombre(plato.getNombre().trim());
        plato.setCategoria(categoria);
        plato.setActivo(true);
        return platoRepository.save(plato);
    }

    @Transactional
    public Plato actualizar(Integer id, Plato datos) {
        Categoria categoria = validarYResolverCategoria(datos);
        Plato plato = obtener(id);
        plato.setNombre(datos.getNombre().trim());
        plato.setDescripcion(datos.getDescripcion());
        plato.setPrecio(datos.getPrecio());
        plato.setImagenUrl(datos.getImagenUrl());
        plato.setCategoria(categoria);
        return platoRepository.save(plato);
    }

    @Transactional
    public Plato desactivar(Integer id) {
        Plato plato = obtener(id);
        plato.setActivo(false);
        return platoRepository.save(plato);
    }

    @Transactional
    public Plato activar(Integer id) {
        Plato plato = obtener(id);
        plato.setActivo(true);
        return platoRepository.save(plato);
    }

    private Categoria validarYResolverCategoria(Plato plato) {
        Map<String, String> errores = new HashMap<>();
        if (plato.getNombre() == null || plato.getNombre().isBlank()) {
            errores.put("nombre", "El nombre es obligatorio");
        }
        if (plato.getPrecio() == null || plato.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            errores.put("precio", "El precio debe ser mayor a 0");
        }

        Categoria categoria = null;
        Integer idCategoria = plato.getCategoria() != null ? plato.getCategoria().getIdCategoria() : null;
        if (idCategoria == null) {
            errores.put("categoria", "Debe seleccionar una categoría");
        } else {
            categoria = categoriaRepository.findById(idCategoria).orElse(null);
            if (categoria == null || !Boolean.TRUE.equals(categoria.getActivo())) {
                errores.put("categoria", "La categoría no existe o esta inactiva");
            }
        }

        if (!errores.isEmpty()) {
            throw ApiException.validacion(errores);
        }
        return categoria;
    }
}

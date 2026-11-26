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
        validar(plato, null);
        Categoria categoria = buscarCategoria(plato);
        plato.setIdPlato(null);
        plato.setNombre(plato.getNombre().trim());
        plato.setCategoria(categoria);
        plato.setActivo(true);
        return platoRepository.save(plato);
    }

    @Transactional
    public Plato actualizar(Integer id, Plato datos) {
        validar(datos, id);
        Categoria categoria = buscarCategoria(datos);
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

    private void validar(Plato plato, Integer idActual) {
        if (plato.getNombre() == null || plato.getNombre().isBlank()) {
            throw ApiException.validacion(Map.of("nombre", "El nombre es obligatorio"));
        }
        if (plato.getPrecio() == null || plato.getPrecio().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.validacion(Map.of("precio", "El precio debe ser mayor a 0"));
        }
        String nombre = plato.getNombre().trim();
        boolean repetido = platoRepository.findByNombreContainingIgnoreCaseAndActivoTrue(nombre).stream()
                .anyMatch(p -> p.getNombre().equalsIgnoreCase(nombre) && !p.getIdPlato().equals(idActual));
        if (repetido) {
            throw ApiException.validacion(Map.of("nombre", "Ya existe un plato activo con ese nombre"));
        }
    }

    private Categoria buscarCategoria(Plato plato) {
        if (plato.getCategoria() == null || plato.getCategoria().getIdCategoria() == null) {
            throw ApiException.validacion(Map.of("categoria", "Debe seleccionar una categoria"));
        }
        Categoria categoria = categoriaRepository.findById(plato.getCategoria().getIdCategoria()).orElse(null);
        if (categoria == null || !Boolean.TRUE.equals(categoria.getActivo())) {
            throw ApiException.validacion(Map.of("categoria", "La categoria no existe o esta inactiva"));
        }
        return categoria;
    }
}

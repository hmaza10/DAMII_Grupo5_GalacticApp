package com.galactica.service;

import com.galactica.entities.Categoria;
import com.galactica.exception.ApiException;
import com.galactica.repository.CategoriaRepository;
import com.galactica.repository.PlatoRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final PlatoRepository platoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, PlatoRepository platoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.platoRepository = platoRepository;
    }

    public List<Categoria> listarActivas() {
        return categoriaRepository.findByActivoTrue();
    }

    public Page<Categoria> listarPaginado(int page, int size, String texto) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 50)),
                Sort.by("idCategoria").ascending());
        if (texto != null && !texto.isBlank()) {
            return categoriaRepository.findByNombreContainingIgnoreCase(texto.trim(), pageable);
        }
        return categoriaRepository.findAll(pageable);
    }

    public Categoria obtener(Integer id) {
        return categoriaRepository.findById(id).orElseThrow(() -> ApiException.noEncontrado("La categoría no existe"));
    }

    public List<Categoria> buscarPorNombre(String texto) {
        return categoriaRepository.findByNombreContainingIgnoreCaseAndActivoTrue(texto);
    }

    @Transactional
    public Categoria crear(Categoria categoria) {
        validar(categoria, null);
        categoria.setIdCategoria(null);
        categoria.setNombre(categoria.getNombre().trim());
        categoria.setActivo(true);
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Categoria actualizar(Integer id, Categoria datos) {
        validar(datos, id);
        Categoria categoria = obtener(id);
        categoria.setNombre(datos.getNombre().trim());
        categoria.setDescripcion(datos.getDescripcion());
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Categoria desactivar(Integer id) {
        Categoria categoria = obtener(id);
        if (!platoRepository.findByCategoria_IdCategoriaAndActivoTrue(id).isEmpty()) {
            throw ApiException.conflicto("No se puede desactivar: la categoría tiene platos activos");
        }
        categoria.setActivo(false);
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Categoria activar(Integer id) {
        Categoria categoria = obtener(id);
        categoria.setActivo(true);
        return categoriaRepository.save(categoria);
    }

    private void validar(Categoria categoria, Integer idActual) {
        Map<String, String> errores = new HashMap<>();
        if (categoria.getNombre() == null || categoria.getNombre().isBlank()) {
            errores.put("nombre", "El nombre es obligatorio");
        } else {
            String nombre = categoria.getNombre().trim();
            boolean repetido = categoriaRepository.findByNombreContainingIgnoreCaseAndActivoTrue(nombre).stream()
                    .anyMatch(c -> c.getNombre().equalsIgnoreCase(nombre) && !c.getIdCategoria().equals(idActual));
            if (repetido) {
                errores.put("nombre", "Ya existe una categoria activa con ese nombre");
            }
        }
        if (!errores.isEmpty()) {
            throw ApiException.validacion(errores);
        }
    }
}

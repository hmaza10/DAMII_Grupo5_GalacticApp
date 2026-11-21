package com.galactica.controller;

import com.galactica.entities.Categoria;
import com.galactica.service.CategoriaService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @PostMapping
    public Categoria crear(@RequestBody Categoria categoria) {
        return categoriaService.crear(categoria);
    }

    @GetMapping
    public List<Categoria> listar() {
        return categoriaService.listarActivas();
    }

    @GetMapping("/paginado")
    public Page<Categoria> listarPaginado(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "5") int size,
                                          @RequestParam(required = false) String texto) {
        return categoriaService.listarPaginado(page, size, texto);
    }

    @GetMapping("/{id}")
    public Categoria obtenerPorId(@PathVariable Integer id) {
        return categoriaService.obtener(id);
    }

    @GetMapping("/buscarPorNombre")
    public List<Categoria> buscarPorNombre(@RequestParam String texto) {
        return categoriaService.buscarPorNombre(texto);
    }

    @PutMapping("/{id}")
    public Categoria actualizar(@PathVariable Integer id,
                                @RequestBody Categoria categoria) {
        return categoriaService.actualizar(id, categoria);
    }

    @DeleteMapping("/{id}")
    public Categoria eliminar(@PathVariable Integer id) {
        return categoriaService.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    public Categoria activar(@PathVariable Integer id) {
        return categoriaService.activar(id);
    }
}

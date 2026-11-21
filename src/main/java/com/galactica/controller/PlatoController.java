package com.galactica.controller;

import com.galactica.entities.Plato;
import com.galactica.service.PlatoService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platos")
public class PlatoController {

    private final PlatoService platoService;

    public PlatoController(PlatoService platoService) {
        this.platoService = platoService;
    }

    @PostMapping
    public Plato crear(@RequestBody Plato plato) {
        return platoService.crear(plato);
    }

    @GetMapping
    public List<Plato> listar() {
        return platoService.listarActivos();
    }

    @GetMapping("/paginado")
    public Page<Plato> listarPaginado(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "5") int size,
                                      @RequestParam(defaultValue = "nombre") String criterio,
                                      @RequestParam(required = false) String texto,
                                      @RequestParam(required = false) Integer idCategoria) {
        return platoService.listarPaginado(page, size, criterio, texto, idCategoria);
    }

    @GetMapping("/{id}")
    public Plato obtenerPorId(@PathVariable Integer id) {
        return platoService.obtener(id);
    }

    @GetMapping("/buscarPorNombre")
    public List<Plato> buscarPorNombre(@RequestParam String texto) {
        return platoService.buscarPorNombre(texto);
    }

    @GetMapping("/buscarPorCategoria")
    public List<Plato> buscarPorCategoria(@RequestParam Integer idCategoria) {
        return platoService.buscarPorCategoria(idCategoria);
    }

    @GetMapping("/buscarPorDescripcion")
    public List<Plato> buscarPorDescripcion(@RequestParam String texto) {
        return platoService.buscarPorDescripcion(texto);
    }

    @PutMapping("/{id}")
    public Plato actualizar(@PathVariable Integer id, @RequestBody Plato plato) {
        return platoService.actualizar(id, plato);
    }

    @DeleteMapping("/{id}")
    public Plato eliminar(@PathVariable Integer id) {
        return platoService.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    public Plato activar(@PathVariable Integer id) {
        return platoService.activar(id);
    }
}

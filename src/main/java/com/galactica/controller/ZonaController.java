package com.galactica.controller;

import com.galactica.entities.Zona;
import com.galactica.service.ZonaService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zonas")
public class ZonaController {

    private final ZonaService zonaService;

    public ZonaController(ZonaService zonaService) {
        this.zonaService = zonaService;
    }

    @PostMapping
    public Zona crear(@RequestBody Zona zona) {
        return zonaService.crear(zona);
    }

    @GetMapping
    public List<Zona> listar() {
        return zonaService.listarActivas();
    }

    @GetMapping("/paginado")
    public Page<Zona> listarPaginado(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "5") int size,
                                     @RequestParam(required = false) String texto) {
        return zonaService.listarPaginado(page, size, texto);
    }

    @GetMapping("/{id}")
    public Zona obtenerPorId(@PathVariable Integer id) {
        return zonaService.obtener(id);
    }

    @GetMapping("/buscarPorNombre")
    public List<Zona> buscarPorNombre(@RequestParam String texto) {
        return zonaService.buscarPorNombre(texto);
    }

    @PutMapping("/{id}")
    public Zona actualizar(@PathVariable Integer id, @RequestBody Zona zona) {
        return zonaService.actualizar(id, zona);
    }

    @DeleteMapping("/{id}")
    public Zona eliminar(@PathVariable Integer id) {
        return zonaService.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    public Zona activar(@PathVariable Integer id) {
        return zonaService.activar(id);
    }

}

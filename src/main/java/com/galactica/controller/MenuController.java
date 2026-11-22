package com.galactica.controller;

import com.galactica.entities.MenuSemanal;
import com.galactica.service.MenuService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/actual")
    public MenuSemanal actual() {
        return menuService.actual();
    }

    @GetMapping("/paginado")
    public Page<MenuSemanal> listarPaginado(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "5") int size,
                                            @RequestParam(required = false) String estado) {
        return menuService.listarPaginado(page, size, estado);
    }

    @GetMapping("/{id}")
    public MenuSemanal obtenerPorId(@PathVariable Integer id, Authentication auth) {
        return menuService.obtener(id, esGestor(auth));
    }

    @PostMapping
    public MenuSemanal crear(@RequestBody MenuSemanal menu) {
        return menuService.crear(menu);
    }

    @PutMapping("/{id}")
    public MenuSemanal actualizar(@PathVariable Integer id, @RequestBody MenuSemanal menu) {
        return menuService.actualizar(id, menu);
    }
    
    @PatchMapping("/{id}/publicar")
    public MenuSemanal publicar(@PathVariable Integer id) {
        return menuService.publicar(id);
    }

    private boolean esGestor(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                || a.getAuthority().equals("ROLE_OPERADOR"));
    }
}

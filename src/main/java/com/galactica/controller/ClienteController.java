package com.galactica.controller;

import com.galactica.entities.Cliente;
import com.galactica.service.ClienteService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    public Cliente crear(@RequestBody Cliente datos) {
        return clienteService.registrar(datos);
    }

    @GetMapping("/me")
    public Cliente miPerfil(Authentication auth) {
        return clienteService.perfil(auth.getName());
    }

    @PutMapping("/me")
    public Cliente actualizarMiPerfil(Authentication auth, @RequestBody Cliente datos) {
        return clienteService.actualizarPerfil(auth.getName(), datos);
    }

    @PutMapping("/me/token")
    public ResponseEntity<Void> registrarToken(Authentication auth, @RequestBody Map<String, String> cuerpo) {
        clienteService.registrarToken(auth.getName(), cuerpo.get("token"));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<Cliente> listar() {
        return clienteService.listarActivos();
    }

    @GetMapping("/paginado")
    public Page<Cliente> listarPaginado(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "5") int size,
                                        @RequestParam(defaultValue = "nombre") String criterio,
                                        @RequestParam(required = false) String texto,
                                        @RequestParam(required = false) Integer idZona) {
        return clienteService.listarPaginado(page, size, criterio, texto, idZona);
    }

    @GetMapping("/{id}")
    public Cliente obtenerPorId(@PathVariable Integer id) {
        return clienteService.obtener(id);
    }

    @GetMapping("/buscarPorNombre")
    public List<Cliente> buscarPorNombre(@RequestParam String texto) {
        return clienteService.buscarPorNombre(texto);
    }

    @PutMapping("/{id}")
    public Cliente actualizar(@PathVariable Integer id, @RequestBody Cliente datos) {
        return clienteService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    public Cliente eliminar(@PathVariable Integer id) {
        return clienteService.desactivar(id);
    }

    @PatchMapping("/{id}/activar")
    public Cliente activar(@PathVariable Integer id) {
        return clienteService.activar(id);
    }
}

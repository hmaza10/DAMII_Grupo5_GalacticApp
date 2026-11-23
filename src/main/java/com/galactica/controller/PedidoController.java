package com.galactica.controller;

import com.galactica.entities.Pedido;
import com.galactica.service.PedidoService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public Pedido crear(Authentication auth, @RequestBody Pedido pedido) {
        return pedidoService.crear(auth.getName(), pedido);
    }

    @GetMapping("/cliente/{idCliente}")
    public List<Pedido> historial(@PathVariable Integer idCliente, Authentication auth) {
        return pedidoService.historial(idCliente, auth.getName(), esGestor(auth));
    }

    @GetMapping("/{id}")
    public Pedido obtenerPorId(@PathVariable Integer id, Authentication auth) {
        return pedidoService.obtener(id, auth.getName(), esGestor(auth));
    }

    @GetMapping
    public Page<Pedido> listarPaginado(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "5") int size,
                                       @RequestParam(required = false) String estado,
                                       @RequestParam(required = false) Integer idCliente,
                                       @RequestParam(required = false) Integer idZona,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        return pedidoService.listarPaginado(page, size, estado, idCliente, idZona, fechaDesde, fechaHasta);
    }

    @PatchMapping("/{id}/estado")
    public Pedido cambiarEstado(@PathVariable Integer id, @RequestParam String estado) {
        return pedidoService.cambiarEstado(id, estado);
    }

    private boolean esGestor(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
        || a.getAuthority().equals("ROLE_OPERADOR"));
    }
}

package com.galactica.service;

import com.galactica.entities.*;
import com.galactica.exception.ApiException;
import com.galactica.repository.MenuSemanalRepository;
import com.galactica.repository.PedidoRepository;
import com.galactica.repository.ZonaRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class PedidoService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final PedidoRepository pedidoRepository;
    private final MenuSemanalRepository menuRepository;
    private final ZonaRepository zonaRepository;
    private final ClienteService clienteService;

    public PedidoService(PedidoRepository pedidoRepository, MenuSemanalRepository menuRepository, ZonaRepository zonaRepository, ClienteService clienteService) {
        this.pedidoRepository = pedidoRepository;
        this.menuRepository = menuRepository;
        this.zonaRepository = zonaRepository;
        this.clienteService = clienteService;
    }

    @Transactional
    public Pedido crear(String login, Pedido datos) {
        Cliente cliente = clienteService.perfil(login);
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw ApiException.conflicto("La cuenta de cliente esta inactiva");
        }

        MenuSemanal menu = buscarMenuPedible(datos);
        Zona zona = buscarZona(datos);
        validarDatosEntrega(datos);

        List<DetallePedido> enviados = datos.getDetalles();
        if (enviados == null || enviados.size() != menu.getDias().size()) {
            throw invalido("detalles", "Debe elegir un plato por cada día del menú");
        }
        if (yaTienePedido(cliente, menu)) {
            throw ApiException.conflicto("Ya tienes un pedido para esa semana");
        }

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setMenuSemanal(menu);
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setDireccionEntrega(datos.getDireccionEntrega().trim());
        pedido.setZona(zona);
        pedido.setHorarioEntrega(datos.getHorarioEntrega().trim());
        pedido.setMetodoPago(datos.getMetodoPago().trim().toUpperCase(Locale.ROOT));
        pedido.setObservaciones(datos.getObservaciones() == null ? null : datos.getObservaciones().trim());

        Set<LocalDate> diasUsados = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (DetallePedido enviado : enviados) {
            DetallePedido detalle = crearDetalle(enviado, menu, diasUsados);
            detalle.setPedido(pedido);
            pedido.getDetalles().add(detalle);
            total = total.add(detalle.getSubtotal());
        }

        BigDecimal tarifa = zona.getTarifaDelivery() == null ? BigDecimal.ZERO : zona.getTarifaDelivery();
        pedido.setTarifaDelivery(tarifa);
        pedido.setTotal(total.add(tarifa).setScale(2, RoundingMode.HALF_UP));
        return pedidoRepository.save(pedido);
    }

    public Pedido obtener(Integer id, String login, boolean gestor) {
        Pedido pedido = buscar(id);
        if (!gestor && !esDelCliente(pedido, login)) {
            throw ApiException.noEncontrado("El pedido no existe");
        }
        return pedido;
    }

    public List<Pedido> historial(Integer idCliente, String login, boolean gestor) {
        if (!gestor && !clienteService.perfil(login).getIdCliente().equals(idCliente)) {
            throw ApiException.noEncontrado("El cliente me existe");
        }
        return pedidoRepository.findByClienteIdClienteOrderByFechaCreacionDesc(idCliente);
    }

    public Page<Pedido> listarPaginado(int page, int size, String estado, Integer idCliente, Integer idZona, LocalDate fechaDesde,
                                       LocalDate fechaHasta) {
        EstadoPedido filtroEstado = (estado == null || estado.isBlank() ? null : parsearEstado(estado));
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw invalido("fechaHasta", "La fecha final no puede ser anterior a la inicial");
        }
        LocalDateTime desde = fechaDesde == null ? null : fechaDesde.atStartOfDay();
        LocalDateTime hasta = fechaHasta == null ? null : fechaHasta.atTime(LocalTime.MAX);

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 50)),
                Sort.by(Sort.Order.desc("fechaCreacion"), Sort.Order.by("idPedido")));
        return pedidoRepository.buscarPaginaConFiltros(filtroEstado, idCliente, idZona, desde, hasta, pageable);
    }

    @Transactional
    public Pedido cambiarEstado(Integer id, String estadoNuevo) {
        Pedido pedido = buscar(id);
        EstadoPedido actual = pedido.getEstado();
        EstadoPedido nuevo = parsearEstado(estadoNuevo);

        if (nuevo == actual) {
            throw ApiException.conflicto("El pedido ya esta " + actual);
        }
        if (!transicionValida(actual, nuevo)) {
            throw ApiException.conflicto("No se puede pasar de " + actual + " a " + nuevo);
        }

        pedido.setEstado(nuevo);
        Pedido guardado = pedidoRepository.save(pedido);
        return guardado;
    }

    private Pedido buscar(Integer id) {
        return pedidoRepository.findById(id).orElseThrow(() -> ApiException.noEncontrado("El pedido no existe"));
    }

    private ApiException invalido(String campo, String mensaje) {
        return ApiException.validacion(Map.of(campo, mensaje));
    }

    private boolean textoInvalido(String texto) {
        return texto == null || texto.isBlank() || texto.trim().length() > 255;
    }

    private boolean esDelCliente(Pedido pedido, String login) {
        return pedido.getCliente() != null && pedido.getCliente().getUsuario() != null &&
                login.equals(pedido.getCliente().getUsuario().getLogin());
    }

    private EstadoPedido parsearEstado(String texto) {
        try {
            return EstadoPedido.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw invalido("estado", "Estado inactivo. Use PENDIENTE, CONFIRMADO, EN_PREPARACION, "
                    + "EN_RUTA, ENTREGADO O CANCELADO");
        }
    }

    private boolean transicionValida(EstadoPedido actual, EstadoPedido nuevo) {
        switch (actual) {
            case PENDIENTE:
                return nuevo == EstadoPedido.CONFIRMADO || nuevo == EstadoPedido.CANCELADO;
            case CONFIRMADO:
                return nuevo == EstadoPedido.EN_PREPARACION || nuevo == EstadoPedido.CANCELADO;
            case EN_PREPARACION:
                return nuevo == EstadoPedido.EN_RUTA;
            case EN_RUTA:
                return nuevo == EstadoPedido.ENTREGADO;
            default:
                return false;
        }
    }

    private MenuSemanal buscarMenuPedible(Pedido datos) {
        if (datos.getMenuSemanal() == null || datos.getMenuSemanal().getIdMenu() == null) {
            throw invalido("menuSemanal", "Debe indicar el menú del pedido");
        }
        MenuSemanal menu = menuRepository.findById(datos.getMenuSemanal().getIdMenu())
                .orElseThrow(() -> ApiException.noEncontrado("El menú no existe"));
        if (menu.getEstado() != EstadoMenu.PUBLICADO) {
            throw ApiException.noEncontrado("El menú no existe");
        }
        if (menu.getFechaInicio().isBefore(LocalDate.now(LIMA))) {
            throw ApiException.conflicto("El pedido para esa semana ya cerro: la semana ya comenzó");
        }
        return menu;
    }

    private Zona buscarZona(Pedido datos) {
        if (datos.getZona() == null || datos.getZona().getIdZona() == null) {
            throw invalido("zona", "Debe seleccionar una zona");
        }
        Zona zona = zonaRepository.findById(datos.getZona().getIdZona()).orElse(null);
        if (zona == null || !Boolean.TRUE.equals(zona.getActivo())) {
            throw invalido("zona", "La zona no existe o esta inactiva");
        }
        return zona;
    }

    private void validarDatosEntrega(Pedido datos) {
        if (textoInvalido(datos.getDireccionEntrega())) {
            throw invalido("direccionEntrega", "La dirección de entrega es obligatoria");
        }
        if (textoInvalido(datos.getHorarioEntrega())) {
            throw invalido("horarioEntrega", "El horario de entrega es obligatorio");
        }
        String pago = datos.getMetodoPago() == null ? "" : datos.getMetodoPago().trim().toUpperCase(Locale.ROOT);
        if (!pago.equals("EFECTIVO") && !pago.equals("YAPE") && !pago.equals("PLIN")) {
            throw invalido("metodoPago", "El método de pago debe ser EFECTIVO, YAPE o PLIN");
        }
        if (datos.getObservaciones() != null && datos.getObservaciones().trim().length() > 255) {
            throw invalido("observaciones", "Las observaciones no pueden pasar de 255 caracteres");
        }
    }

    private boolean yaTienePedido(Cliente cliente, MenuSemanal menu) {
        return pedidoRepository.findByClienteIdClienteOrderByFechaCreacionDesc(cliente.getIdCliente()).stream()
                .anyMatch(p -> p.getMenuSemanal() != null && p.getMenuSemanal().getIdMenu().equals(menu.getIdMenu())
                && p.getEstado() != EstadoPedido.CANCELADO);
    }

    private DetallePedido crearDetalle(DetallePedido enviado, MenuSemanal menu, Set<LocalDate> diasUsados) {
        if (enviado == null || enviado.getFechaConsumo() == null) {
            throw invalido("detalles", "Cada plato debe indicar su fecha de consumo");
        }
        MenuDia dia = buscarDia(menu, enviado.getFechaConsumo());
        if (!diasUsados.add(dia.getFecha())) {
            throw invalido("detalles", dia.getDiaSemana() + ": el día esta repetido");
        }
        if (enviado.getCantidad() != null && enviado.getCantidad() != 1) {
            throw invalido("detalles", dia.getDiaSemana() + ": solo se permite una porción por día");
        }
        Plato plato = elegirOpcion(dia, enviado);

        DetallePedido detalle = new DetallePedido();
        detalle.setFechaConsumo(dia.getFecha());
        detalle.setDiaSemana(dia.getDiaSemana());
        detalle.setPlato(plato);
        detalle.setCantidad(1);
        detalle.setPrecioUnitario(plato.getPrecio());
        detalle.setSubtotal(plato.getPrecio());
        return detalle;
    }

    private MenuDia buscarDia(MenuSemanal menu, LocalDate fecha) {
        for (MenuDia dia : menu.getDias()) {
            if (dia.getFecha().equals(fecha)) {
                return dia;
            }
        }
        throw invalido("detalles", "La fecha " + fecha + " no pertenece al menú");
    }

    private Plato elegirOpcion(MenuDia dia, DetallePedido enviado) {
        if (enviado.getPlato() == null || enviado.getPlato().getIdPlato() == null) {
            throw invalido("detalles", dia.getDiaSemana() + ": debe elegir un plato");
        }
        Integer idPlato = enviado.getPlato().getIdPlato();
        Plato plato;
        if (dia.getOpcion1() != null && dia.getOpcion1().getIdPlato().equals(idPlato)) {
            plato = dia.getOpcion1();
        } else if (dia.getOpcion2() != null && dia.getOpcion2().getIdPlato().equals(idPlato)) {
            plato = dia.getOpcion2();
        } else {
            throw invalido("detalles", dia.getDiaSemana() + ": el plato no es una opción de ese día");
        }
        if (!Boolean.TRUE.equals(plato.getActivo())) {
            throw invalido("detalles", dia.getDiaSemana() + ": el plato ya no esta disponible");
        }
        return plato;
    }

}

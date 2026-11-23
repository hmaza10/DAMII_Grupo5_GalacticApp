package com.galactica.config;

import com.galactica.entities.*;
import com.galactica.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {
    private final TipoRepository tipoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ZonaRepository zonaRepository;
    private final ClienteRepository clienteRepository;
    private final CategoriaRepository categoriaRepository;
    private final PlatoRepository platoRepository;
    private final MenuSemanalRepository menuSemanalRepository;
    private final PedidoRepository pedidoRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(TipoRepository tipoRepository, UsuarioRepository usuarioRepository, ZonaRepository zonaRepository,
                      ClienteRepository clienteRepository, CategoriaRepository categoriaRepository, PlatoRepository platoRepository,
                      MenuSemanalRepository menuSemanalRepository, PedidoRepository pedidoRepository, PasswordEncoder passwordEncoder) {
        this.tipoRepository = tipoRepository;
        this.usuarioRepository = usuarioRepository;
        this.zonaRepository = zonaRepository;
        this.clienteRepository = clienteRepository;
        this.categoriaRepository = categoriaRepository;
        this.platoRepository = platoRepository;
        this.menuSemanalRepository = menuSemanalRepository;
        this.pedidoRepository = pedidoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @Override
    public void run(String...args) {
        if (tipoRepository.count() > 0)
            return;

        Tipo admin = new Tipo();
        admin.setIdTipo(1);
        admin.setDescripcion("administrador");
        tipoRepository.save(admin);

        Tipo tipoCliente = new Tipo();
        tipoCliente.setIdTipo(2);
        tipoCliente.setDescripcion("cliente");
        tipoRepository.save(tipoCliente);

        Usuario adminUser = new Usuario();
        adminUser.setNombres("Valerie");
        adminUser.setApellidos("Cavero");
        adminUser.setLogin("admin");
        adminUser.setClave(passwordEncoder.encode("admin123"));
        adminUser.setEmail("vcavero@galacticadelivery.com");
        adminUser.setActivo(true);
        adminUser.setTipo(admin);
        usuarioRepository.save(adminUser);

        Zona z1 = nuevaZona("San Isidro", "0.00");
        Zona z2 = nuevaZona("Miraflores", "3.00");
        Zona z3 = nuevaZona("Surco", "5.00");
        Zona z4 = nuevaZona("La Molina", "6.00");

        List<Cliente> clientes = new ArrayList<>();
        clientes.add(nuevoCliente(nuevoUsuario("Carlos", "Ramirez", "cliente", "cliente123", "cliente@galacticadelivery.com", tipoCliente),
                "987654321", "Av. Javier Prado Este 123, Dpto 501", z1, "Frente al parque"));
        clientes.add(nuevoCliente(
                nuevoUsuario("Lucia", "Fernandez", "lucia", "lucia123", "lucia@galacticadelivery.com", tipoCliente),
                "956741258", "Calle Los Pinos 245", z2, "Porton negro"));
        clientes.add(nuevoCliente(
                nuevoUsuario("Jorge", "Paredes", "jorge", "jorge123", "jorge@galacticadelivery.com", tipoCliente),
                "965412387", "Av. Caminos del Inca 890", z3, "Edificio azul, piso 3"));
        clientes.add(nuevoCliente(
                nuevoUsuario("Andrea", "Salazar", "andrea", "andrea123", "andrea@galacticadelivery.com", tipoCliente),
                "974852963", "Calle Las Orquideas 120", z4, "Casa de dos pisos"));

        Categoria c1 = nuevaCategoria("Platos de fondo", "Almuerzos principales balanceados");
        Categoria c2 = nuevaCategoria("Bowls", "Bowls con proteina y verduras");
        Categoria c3 = nuevaCategoria("Ensaladas", "Ensaladas frescas con proteina");
        Categoria c4 = nuevaCategoria("Sopas y cremas", "Entradas calientes ligeras");

        List<Plato> platos = new ArrayList<>();
        platos.add(nuevoPlato(c1, "Pollo al horno con quinua", "Pechuga al horno con quinua y verduras", "14.00"));
        platos.add(nuevoPlato(c1, "Lomo saltado light", "Lomo fino con verduras salteadas y arroz integral", "16.00"));
        platos.add(nuevoPlato(c1, "Pescado a la plancha con camote", "Filete de pescado con camote y ensalada", "17.00"));
        platos.add(nuevoPlato(c1, "Arroz con pollo saludable", "Arroz verde con pollo y zarza criolla", "14.50"));
        platos.add(nuevoPlato(c1, "Tallarin saltado de verduras", "Fideos integrales con verduras al wok", "13.50"));
        platos.add(nuevoPlato(c2, "Bowl de quinua y palta", "Quinua, palta, tomate y huevo", "15.00"));
        platos.add(nuevoPlato(c2, "Bowl de pollo teriyaki", "Pollo teriyaki con arroz y brocoli", "16.00"));
        platos.add(nuevoPlato(c3, "Ensalada Cesar con pollo", "Lechuga, pollo grillado y aderezo ligero", "13.00"));
        platos.add(nuevoPlato(c3, "Ensalada mediterranea", "Tomate, pepino, aceituna y queso fresco", "12.50"));
        platos.add(nuevoPlato(c4, "Crema de zapallo", "Crema de zapallo con tostadas integrales", "9.00"));
        platos.add(nuevoPlato(c4, "Sopa de verduras", "Caldo con verduras de estacion", "8.50"));
        platos.add(nuevoPlato(c4, "Aguadito de pollo", "Aguadito tradicional con menos grasa", "12.00"));

        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        MenuSemanal menuPasado = nuevoMenu(lunes.minusWeeks(1), EstadoMenu.PUBLICADO, platos, 4);
        MenuSemanal menuActual = nuevoMenu(lunes, EstadoMenu.PUBLICADO, platos, 0);
        nuevoMenu(lunes.plusWeeks(1), EstadoMenu.BORRADOR, platos, 2);

        seedPedidos(clientes, menuPasado, menuActual);
        sembrarPedidosDemo(clientes, menuActual);
    }

    private void seedPedidos(List<Cliente> clientes, MenuSemanal menuPasado, MenuSemanal menuActual) {
        crearPedido(clientes.get(0), menuPasado, EstadoPedido.ENTREGADO, "EFECTIVO", "Sin ajo, por favor", 1, 1, 2, 1, 2);
        crearPedido(clientes.get(1), menuPasado, EstadoPedido.ENTREGADO, "YAPE", "Tocar el timbre dos veces", 2, 2, 1, 1, 1);
        crearPedido(clientes.get(2), menuPasado, EstadoPedido.CANCELADO, "PLIN", "Cancelado por el cliente", 1, 2, 1, 2, 1);
        crearPedido(clientes.get(0), menuActual, EstadoPedido.CONFIRMADO, "YAPE", "Entrega en recepcion", 2, 1, 1, 2, 2);
        crearPedido(clientes.get(1), menuActual, EstadoPedido.PENDIENTE, "EFECTIVO", "Pedido de esta semana", 1, 1, 1, 1, 1);
    }

    private Pedido crearPedido(Cliente cliente, MenuSemanal menu, EstadoPedido estado,
                               String metodoPago, String observaciones, int... secuencia) {
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setMenuSemanal(menu);
        pedido.setEstado(estado);
        pedido.setDireccionEntrega(cliente.getDireccion());
        pedido.setZona(cliente.getZona());
        pedido.setHorarioEntrega("12:00 - 13:00");
        pedido.setMetodoPago(metodoPago);
        pedido.setObservaciones(observaciones);

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < menu.getDias().size(); i++) {
            MenuDia dia = menu.getDias().get(i);
            Plato plato = secuencia[i] == 1 ? dia.getOpcion1() : dia.getOpcion2();

            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido);
            detalle.setFechaConsumo(dia.getFecha());
            detalle.setDiaSemana(dia.getDiaSemana());
            detalle.setPlato(plato);
            detalle.setCantidad(1);
            detalle.setPrecioUnitario(plato.getPrecio());
            detalle.setSubtotal(plato.getPrecio());
            pedido.getDetalles().add(detalle);

            total = total.add(detalle.getSubtotal());
        }
        BigDecimal tarifa = cliente.getZona().getTarifaDelivery();
        pedido.setTarifaDelivery(tarifa);
        pedido.setTotal(total.add(tarifa));
        return pedidoRepository.save(pedido);
    }

    private Zona nuevaZona(String nombre, String tarifa) {
        Zona z = new Zona();
        z.setNombre(nombre);
        z.setTarifaDelivery(new BigDecimal(tarifa));
        z.setActivo(true);
        return zonaRepository.save(z);
    }

    private Usuario nuevoUsuario(String nombres, String apellidos, String login, String clave,
                                 String email, Tipo tipo) {
        Usuario u = new Usuario();
        u.setNombres(nombres);
        u.setApellidos(apellidos);
        u.setLogin(login);
        u.setClave(passwordEncoder.encode(clave));
        u.setEmail(email);
        u.setActivo(true);
        u.setTipo(tipo);
        return usuarioRepository.save(u);
    }

    private Cliente nuevoCliente(Usuario usuario, String telefono, String direccion, Zona zona, String referencia) {
        Cliente c = new Cliente();
        c.setUsuario(usuario);
        c.setTelefono(telefono);
        c.setDireccion(direccion);
        c.setZona(zona);
        c.setReferencia(referencia);
        c.setActivo(true);
        return clienteRepository.save(c);
    }

    private Categoria nuevaCategoria(String nombre, String descripcion) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setDescripcion(descripcion);
        c.setActivo(true);
        return categoriaRepository.save(c);
    }

    private Plato nuevoPlato(Categoria categoria, String nombre, String descripcion, String precio) {
        Plato p = new Plato();
        p.setCategoria(categoria);
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(new BigDecimal(precio));
        p.setActivo(true);
        return platoRepository.save(p);
    }

    private MenuSemanal nuevoMenu(LocalDate lunes, EstadoMenu estado, List<Plato> platos, int desplazamiento) {
        MenuSemanal menu = new MenuSemanal();
        menu.setFechaInicio(lunes);
        menu.setFechaFin(lunes.plusDays(4));
        menu.setEstado(estado);
        if (estado == EstadoMenu.PUBLICADO) {
            menu.setPublicadoEn(lunes.minusDays(1).atTime(18, 0));
        }

        String[] dias = {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES"};
        for (int i = 0; i < dias.length; i++) {
            MenuDia dia = new MenuDia();
            dia.setMenuSemanal(menu);
            dia.setFecha(lunes.plusDays(i));
            dia.setDiaSemana(dias[i]);
            dia.setOpcion1(platos.get((i * 2 + desplazamiento) % platos.size()));
            dia.setOpcion2(platos.get((i * 2 + 1 + desplazamiento) % platos.size()));
            menu.getDias().add(dia);
        }
        return menuSemanalRepository.save(menu);
    }

    private void sembrarPedidosDemo(List<Cliente> clientes, MenuSemanal menu) {
        EstadoPedido[] estados = EstadoPedido.values();
        String[] metodos = {"EFECTIVO", "YAPE", "PLIN"};
        for (int i = 0; i < 12; i++) {
            Cliente cliente = clientes.get(i % clientes.size());
            int a = (i % 2) + 1;
            crearPedido(cliente, menu, estados[i % estados.length], metodos[i % metodos.length],
                    "Pedido demo N-" + (i + 1), a, 3 - a, a, a, 3 - a);
        }
    }
}

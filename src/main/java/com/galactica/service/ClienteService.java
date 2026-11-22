package com.galactica.service;

import com.galactica.entities.*;
import com.galactica.exception.ApiException;
import com.galactica.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ClienteService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern LOGIN = Pattern.compile("^[A-Za-z0-9._-]{4,30}$");
    private static final Pattern TELEFONO = Pattern.compile("^\\d{9}$");

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ZonaRepository zonaRepository;
    private final TipoRepository tipoRepository;
    private final PedidoRepository pedidoRepository;
    private final PasswordEncoder passwordEncoder;

    public ClienteService(ClienteRepository clienteRepository, UsuarioRepository usuarioRepository, ZonaRepository zonaRepository, TipoRepository tipoRepository, PedidoRepository pedidoRepository, PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.zonaRepository = zonaRepository;
        this.tipoRepository = tipoRepository;
        this.pedidoRepository = pedidoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Cliente registrar(Cliente datos) {
        Map<String, String> errores = new HashMap<>();
        Usuario u = datos.getUsuario();
        if (u == null) {
            errores.put("usuario", "Los datos de la cuenta son obligatorios");
            throw ApiException.validacion(errores);
        }

        String login = u.getLogin() == null ? "" : u.getLogin().trim();
        if (vacio(u.getNombres())) {
            errores.put("nombres", "Los nombres son obligatorios");
        }
        if (vacio(u.getApellidos())) {
            errores.put("apellidos", "Los apellidos son obligatorios");
        }
        if (!LOGIN.matcher(login).matches()) {
            errores.put("login", "El usuario debe tener de 4 a 30 caracteres(letras, números, punto, guión)");
        }
        if (vacio(u.getEmail()) || !EMAIL.matcher(u.getEmail().trim()).matches()) {
            errores.put("email", "Ingrese un correo válido");
        }
        validarDatosEntrega(datos, errores);
        Zona zona = resolverZona(datos, errores);
        if (!errores.isEmpty()) {
            throw ApiException.validacion(errores);
        }
        if (usuarioRepository.findByLogin(login).isPresent()) {
            throw ApiException.conflicto("El usuario ya existe");
        }
        Tipo tipoCliente = tipoRepository.findByDescripcionIgnoreCase("cliente")
                .orElseThrow(() -> new IllegalStateException("No existe el tipo de usuario 'cliente'"));
        Usuario usuario = new Usuario();
        usuario.setNombres(u.getNombres().trim());
        usuario.setApellidos(u.getApellidos().trim());
        usuario.setLogin(login);
        usuario.setClave(passwordEncoder.encode(u.getClave()));
        usuario.setEmail(u.getEmail().trim());
        usuario.setActivo(true);
        usuario.setTipo(tipoCliente);
        usuarioRepository.save(usuario);

        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario);
        cliente.setTelefono(datos.getTelefono().trim());
        cliente.setDireccion(datos.getDireccion().trim());
        cliente.setReferencia(datos.getReferencia());
        cliente.setZona(zona);
        cliente.setActivo(true);
        return clienteRepository.save(cliente);
    }

    public Cliente perfil(String login) {
        return clientePorLogin(login);
    }

    @Transactional
    public Cliente actualizarPerfil(String login, Cliente datos) {
        return aplicarCambios(clientePorLogin(login), datos);
    }

    @Transactional
    public void registrarToken(String login, String token) {
        if (vacio(token)) {
            throw ApiException.validacion(Map.of("token", "El token es obligatorio"));
        }
        if (token.trim().length() > 255) {
            throw ApiException.validacion(Map.of("token", "El token es demasiado largo"));
        }
        Cliente cliente = clientePorLogin(login);
        cliente.setFcmToken(token.trim());
        clienteRepository.save(cliente);
    }

    public List<Cliente> listarActivos() {
        return clienteRepository.findByActivoTrue();
    }

    public Page<Cliente> listarPaginado(int page, int size, String criterio, String texto, Integer idZona) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 50)),
                Sort.by("idCliente").ascending());
        boolean hayTexto = texto != null && !texto.isBlank();
        switch (criterio) {
            case "telefono":
                if (hayTexto) {
                    return clienteRepository.findByTelefonoContaining(texto.trim(), pageable);
                }
                break;
            case "zona":
                if (idZona != null) {
                    return clienteRepository.findByZonaIdZona(idZona, pageable);
                }
                break;
            case "nombre":
            default:
                if (hayTexto) {
                    return clienteRepository.buscarPorNombrePaginado(texto.trim(), pageable);
                }
                break;
        }
        return clienteRepository.findAll(pageable);
    }

    public Cliente obtener(Integer id) {
        return clienteRepository.findById(id).orElseThrow(() -> ApiException.noEncontrado("El cliente no existe"));
    }

    public List<Cliente> buscarPorNombre(String texto) {
        return clienteRepository.buscarPorNombre(texto);
    }

    @Transactional
    public Cliente actualizar(Integer id, Cliente datos) {
        return aplicarCambios(obtener(id), datos);
    }

    @Transactional
    public Cliente desactivar(Integer id) {
        Cliente cliente = obtener(id);
        boolean conPedidoEnCurso = pedidoRepository.findByClienteIdClienteOrderByFechaCreacionDesc(id).stream()
                .anyMatch(p -> p.getEstado() != EstadoPedido.ENTREGADO && p.getEstado() != EstadoPedido.CANCELADO);
        if (conPedidoEnCurso) {
            throw ApiException.conflicto("No existe desactivar: el cliente tiene pedidos en curso");
        }
        cambiarEstado(cliente, false);
        return cliente;
    }

    @Transactional
    public Cliente activar(Integer id) {
        Cliente cliente = obtener(id);
        if (cliente.getZona() == null || !Boolean.TRUE.equals(cliente.getZona().getActivo())) {
            throw ApiException.conflicto("La zona del cliente esta inactiva: asigne otra zona antes de activarlo");
        }
        cambiarEstado(cliente, true);
        return cliente;
    }

    private Cliente clientePorLogin(String login) {
        return usuarioRepository.findByLogin(login).flatMap(u -> clienteRepository.findByUsuarioIdUsuario(u.getIdUsuario()))
                .orElseThrow(() -> ApiException.noEncontrado("No existe un perfil de cliente para este usuario"));
    }

    private void cambiarEstado(Cliente cliente, boolean activo) {
        cliente.setActivo(activo);
        clienteRepository.save(cliente);
        if (cliente.getUsuario() != null) {
            cliente.getUsuario().setActivo(activo);
            usuarioRepository.save(cliente.getUsuario());
        }
    }

    private Cliente aplicarCambios(Cliente cliente, Cliente datos) {
        Map<String, String> errores = new HashMap<>();
        validarDatosEntrega(datos, errores);
        Zona zona = resolverZona(datos, errores);

        Usuario du = datos.getUsuario();
        if (du != null) {
            if (du.getNombres() != null && du.getNombres().isBlank()) {
                errores.put("nombres", "Los nombres no pueden estar vacíos");
            }
            if (du.getApellidos() != null && du.getApellidos().isBlank()) {
                errores.put("apellidos", "Los apellidos no pueden estar vacíos");
            }
            if (du.getEmail() != null && !EMAIL.matcher(du.getEmail().trim()).matches()) {
                errores.put("email", "Ingrese un correo válido");
            }
        }
        if (!errores.isEmpty()) {
            throw ApiException.validacion(errores);
        }

        cliente.setTelefono(datos.getTelefono().trim());
        cliente.setDireccion(datos.getDireccion().trim());
        cliente.setReferencia(datos.getReferencia());
        cliente.setZona(zona);

        if (du != null && cliente.getUsuario() != null) {
            Usuario usuario = cliente.getUsuario();
            if (du.getNombres() != null) {
                usuario.setNombres(du.getNombres().trim());
            }
            if (du.getApellidos() != null) {
                usuario.setEmail(du.getApellidos().trim());
            }
            if (du.getEmail() != null) {
                usuario.setEmail(du.getEmail().trim());
            }
            usuarioRepository.save(usuario);
        }
        return clienteRepository.save(cliente);
    }

    private void validarDatosEntrega(Cliente datos, Map<String, String> errores) {
        if (datos.getTelefono() == null || !TELEFONO.matcher(datos.getTelefono().trim()).matches()) {
            errores.put("telefono", "El telefono debe tener 9 dígitos");
        }
        if (vacio(datos.getDireccion())) {
            errores.put("direccion", "La dirección es obligatoria");
        }
    }

    private Zona resolverZona(Cliente datos, Map<String, String> errores) {
        Integer idZona = datos.getZona() != null ? datos.getZona().getIdZona() : null;
        if (idZona == null) {
            errores.put("zona", "Debe seleccionar una zona");
            return null;
        }
        Zona zona = zonaRepository.findById(idZona).orElse(null);
        if (zona == null || !Boolean.TRUE.equals(zona.getActivo())) {
            errores.put("zona", "La zona no existe o esta inactiva");
            return null;
        }
        return zona;
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}

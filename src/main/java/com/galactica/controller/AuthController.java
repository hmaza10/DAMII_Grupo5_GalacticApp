package com.galactica.controller;

import com.galactica.dto.LoginRequest;
import com.galactica.entities.Cliente;
import com.galactica.entities.Tipo;
import com.galactica.entities.Usuario;
import com.galactica.repository.ClienteRepository;
import com.galactica.repository.TipoRepository;
import com.galactica.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final TipoRepository tipoRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UsuarioRepository usuarioRepository, TipoRepository tipoRepository, ClienteRepository clienteRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.tipoRepository = tipoRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Usuario usuario = usuarioRepository.findByLogin(loginRequest.getLogin()).orElse(null);
        if (usuario == null || !passwordEncoder.matches(loginRequest.getClave(), usuario.getClave())) {
            return ResponseEntity.status(401).body("Credenciales invalidas");
        }
        return ResponseEntity.ok(usuario);
    }

    @PostMapping("/registro")
    @Transactional
    public ResponseEntity<?> registro(@RequestBody Cliente cliente) {
        Usuario usuario = cliente.getUsuario();
        if (usuario == null || vacio(usuario.getNombres()) || vacio(usuario.getApellidos())
                || vacio(usuario.getLogin()) || vacio(usuario.getClave())
                || vacio(usuario.getEmail()) || vacio(cliente.getTelefono())) {
            return ResponseEntity.status(400).body("Datos obligatorios incompletos");
        }
        if (usuarioRepository.findByLogin(usuario.getLogin()).isPresent()) {
            return ResponseEntity.status(409).body("El usuario ya existe");
        }
        Tipo tipoCliente = tipoRepository.findByDescripcionIgnoreCase("cliente").orElse(null);
        if (tipoCliente == null) {
            return ResponseEntity.status(500).body("No existe el tipo de usuario cliente");
        }

        usuario.setIdUsuario(null);
        usuario.setClave(passwordEncoder.encode(usuario.getClave()));
        usuario.setActivo(true);
        usuario.setTipo(tipoCliente);
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(clienteRepository.save(cliente));
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}

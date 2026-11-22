package com.galactica.controller;

import com.galactica.dto.LoginRequest;
import com.galactica.entities.Cliente;
import com.galactica.entities.Tipo;
import com.galactica.entities.Usuario;
import com.galactica.repository.ClienteRepository;
import com.galactica.repository.TipoRepository;
import com.galactica.repository.UsuarioRepository;
import com.galactica.service.ClienteService;
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
    private final ClienteService clienteService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UsuarioRepository usuarioRepository, TipoRepository tipoRepository, ClienteRepository clienteRepository, ClienteService clienteService, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.clienteService = clienteService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Usuario usuario = usuarioRepository.findByLogin(loginRequest.getLogin()).orElse(null);
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())
                || !passwordEncoder.matches(loginRequest.getClave(), usuario.getClave())) {
            return ResponseEntity.status(401).body("Credenciales invalidas");
        }
        return ResponseEntity.ok(usuario);
    }

    @PostMapping("/registro")
    @Transactional
    public ResponseEntity<?> registro(@RequestBody Cliente cliente) {
        return ResponseEntity.ok(clienteService.registrar(cliente));
    }
}

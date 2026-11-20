package com.galactica.service;

import com.galactica.entities.Usuario;
import com.galactica.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + login));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw  new UsernameNotFoundException("Usuario inactivo: " + login);
        }

        String descripcion = usuario.getTipo().getDescripcion();
        String rol = "administrador".equalsIgnoreCase(descripcion)
                ? "ADMIN"
                : "operador".equalsIgnoreCase(descripcion)
                ? "OPERADOR"
                : "CLIENTE";

        return org.springframework.security.core.userdetails.User.builder()
                .username(usuario.getLogin())
                .password(usuario.getClave())
                .roles(rol)
                .build();
    }
}

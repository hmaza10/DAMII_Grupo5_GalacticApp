package com.galactica.repository;

import com.galactica.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByLoginAndClave(String login, String clave);
    Optional<Usuario> findByLogin(String login);
}

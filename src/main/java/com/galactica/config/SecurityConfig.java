package com.galactica.config;

import org.springframework.web.filter.CorsFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/registro").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/zonas").permitAll()
                        .requestMatchers("/api/clientes/me/**").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/menus/actual").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/menus/paginado").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/menus/*").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/pedidos").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/api/pedidos/cliente/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/pedidos/*/pdf").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/pedidos/*").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/pedidos").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/pedidos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reportes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/clientes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/platos/**", "/api/categorias/**", "/api/menus/**", "/api/zonas/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/platos/**", "/api/categorias/**", "/api/menus/**", "/api/clientes/**", "/api/zonas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/platos/**", "/api/categorias/**", "/api/menus/**", "/api/clientes/**", "/api/zonas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/platos/**", "/api/categorias/**", "/api/menus/**", "/api/clientes/**", "/api/zonas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/platos/**", "/api/categorias/**", "/api/menus/**", "/api/clientes/**", "/api/zonas/**").hasRole("ADMIN")
                        .anyRequest().authenticated()).httpBasic(Customizer.withDefaults()).build();
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}

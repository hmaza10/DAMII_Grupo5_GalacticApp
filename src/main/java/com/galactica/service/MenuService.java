package com.galactica.service;

import com.galactica.entities.EstadoMenu;
import com.galactica.entities.MenuDia;
import com.galactica.entities.MenuSemanal;
import com.galactica.entities.Plato;
import com.galactica.exception.ApiException;
import com.galactica.repository.MenuSemanalRepository;
import com.galactica.repository.PlatoRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.List;

@Service
public class MenuService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final String[] DIAS = {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES"};

    private final MenuSemanalRepository menuRepository;
    private final PlatoRepository platoRepository;

    public MenuService(MenuSemanalRepository menuRepository, PlatoRepository platoRepository) {
        this.menuRepository = menuRepository;
        this.platoRepository = platoRepository;
    }

    public MenuSemanal actual() {
        return menuRepository.findFirstByEstadoAndFechaFinGreaterThanEqualOrderByFechaInicioAsc(EstadoMenu.PUBLICADO, hoy())
                .orElseThrow(() -> ApiException.noEncontrado("Aún no hay un menú publicado"));
    }

    public MenuSemanal obtener(Integer id, boolean gestor) {
        MenuSemanal menu = buscar(id);
        if (!gestor && menu.getEstado() != EstadoMenu.PUBLICADO) {
            throw ApiException.noEncontrado("El menú no existe");
        }
        return menu;
    }

    public Page<MenuSemanal> listarPaginado(int page, int size, String estado) {
        int pagina = Math.max(page, 0);
        int tamano = Math.max(1, Math.min(size, 50));
        if (estado == null || estado.isBlank()) {
            return menuRepository.findAllByOrderByFechaInicioDesc(PageRequest.of(pagina, tamano));
        }
        EstadoMenu filtro;
        try {
            filtro = EstadoMenu.valueOf(estado.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw invalido("estado", "Estado inválido: use BORRADOR o PUBLICADO");
        }
        return menuRepository.findByEstado(filtro, PageRequest.of(pagina, tamano, Sort.by("fechaInicio").descending()));
    }

    @Transactional
    public MenuSemanal crear(MenuSemanal datos) {
        validarSemana(datos.getFechaInicio(), null);

        MenuSemanal menu = new MenuSemanal();
        menu.setFechaInicio(datos.getFechaInicio());
        menu.setFechaFin(datos.getFechaInicio().plusDays(4));
        menu.setEstado(EstadoMenu.BORRADOR);
        armarDias(menu, datos.getDias());
        return menuRepository.save(menu);
    }

    @Transactional
    public MenuSemanal actualizar(Integer id, MenuSemanal datos) {
        MenuSemanal menu = buscar(id);
        if (menu.getEstado() == EstadoMenu.PUBLICADO) {
            throw ApiException.conflicto("Un menú publicado no se puede editar");
        }
        validarSemana(datos.getFechaInicio(), menu);

        menu.setFechaInicio(datos.getFechaInicio());
        menu.setFechaFin(datos.getFechaInicio().plusDays(4));
        armarDias(menu, datos.getDias());
        return menuRepository.save(menu);
    }

    @Transactional
    public MenuSemanal publicar(Integer id) {
        MenuSemanal menu = buscar(id);
        if (menu.getEstado() == EstadoMenu.PUBLICADO) {
            throw ApiException.conflicto("El menú ya está publicado");
        }
        if (menu.getFechaFin().isBefore(hoy())) {
            throw ApiException.conflicto("No se puede publicar: la semana ya termino");
        }
        if (menuRepository.existsByEstadoAndFechaInicio(EstadoMenu.PUBLICADO, menu.getFechaInicio())) {
            throw  ApiException.conflicto("Ya hay un menú publicado para esa semana");
        }
        if (menu.getDias().size() != 5) {
            throw invalido("dias", "El menú debe tener los 5 días completos");
        }
        for (MenuDia dia : menu.getDias()) {
            if (!estaActivo(dia.getOpcion1()) || !estaActivo(dia.getOpcion2())) {
                throw invalido("dias", dia.getDiaSemana() + ": hay un plato inactivo, elija otro");
            }
        }

        menu.setEstado(EstadoMenu.PUBLICADO);
        menu.setPublicadoEn(LocalDateTime.now(LIMA));
        MenuSemanal publicado = menuRepository.save(menu);
        return publicado;
    }

    private LocalDate hoy() {
        return LocalDate.now(LIMA);
    }

    private MenuSemanal buscar(Integer id) {
        return menuRepository.findById(id).orElseThrow(() -> ApiException.noEncontrado("El menú no existe"));
    }

    private ApiException invalido(String campo, String mensaje) {
        return ApiException.validacion(Map.of(campo, mensaje));
    }

    private boolean estaActivo(Plato plato) {
        return plato != null && Boolean.TRUE.equals(plato.getActivo());
    }

    private void validarSemana(LocalDate inicio, MenuSemanal actual) {
        if (inicio == null) {
            throw invalido("fechaInicio", "La fecha de inicio es obligatoria");
        }
        if (inicio.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw invalido("fechaInicio", "La semana debe iniciar un lunes");
        }
        boolean cambiaSemana = actual == null || !inicio.equals(actual.getFechaInicio());
        if (cambiaSemana && (menuRepository.existsByEstadoAndFechaInicio(EstadoMenu.BORRADOR, inicio) ||
                menuRepository.existsByEstadoAndFechaInicio(EstadoMenu.PUBLICADO, inicio))) {
            throw invalido("fechaInicio", "Ya existe un menú para esa semana");
        }
    }

    private void armarDias(MenuSemanal menu, List<MenuDia> enviados) {
        if (enviados == null || enviados.size() != 5) {
            throw invalido("dias", "El menú debe tener 5 días, de lunes a viernes, en ese orden");
        }
        List<MenuDia> nuevos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            MenuDia enviado = enviados.get(i);
            Plato opcion1 = buscarPlato(enviado == null ? null : enviado.getOpcion1(), i);
            Plato opcion2 = buscarPlato(enviado == null ? null : enviado.getOpcion2(), i);
            if (opcion1.getIdPlato().equals(opcion2.getIdPlato())) {
                throw invalido("dias", DIAS[i] + ": las dos opciones deben ser distintas");
            }

            MenuDia dia = new MenuDia();
            dia.setMenuSemanal(menu);
            dia.setFecha(menu.getFechaInicio().plusDays(i));
            dia.setDiaSemana(DIAS[i]);
            dia.setOpcion1(opcion1);
            dia.setOpcion2(opcion2);
            nuevos.add(dia);
        }
        menu.getDias().clear();
        menu.getDias().addAll(nuevos);
    }

    private Plato buscarPlato(Plato referencia, int posicionDia) {
        if (referencia == null || referencia.getIdPlato() == null) {
            throw invalido("dias", DIAS[posicionDia] + ": debe seleccionar un plato");
        }
        Plato plato = platoRepository.findById(referencia.getIdPlato()).orElse(null);
        if (!estaActivo(plato)) {
            throw invalido("dias", DIAS[posicionDia] + ": el plato no existe o esta inactivo");
        }
        return plato;
    }
}

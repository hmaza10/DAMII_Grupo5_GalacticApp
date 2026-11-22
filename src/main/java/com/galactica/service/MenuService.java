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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        int p = Math.max(page, 0);
        int s = Math.max(1, Math.min(size, 50));
        if (estado == null || estado.isBlank()) {
            return menuRepository.findAllByOrderByFechaInicioDesc(PageRequest.of(p, s));
        }
        EstadoMenu filtro;
        try {
            filtro = EstadoMenu.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.validacion(Map.of("estado", "Estado inválido: use BORRADOR o PUBLICADO"));
        }
        return menuRepository.findByEstado(filtro, PageRequest.of(p, s, Sort.by("fechaInicio").descending()));
    }

    @Transactional
    public MenuSemanal crear(MenuSemanal datos) {
        Map<String, String> errores = new HashMap<>();
        validarSemana(datos.getFechaInicio(), null, errores);
        List<Plato[]> opciones = resolverOpciones(datos, errores);
        if (!errores.isEmpty()) {
            throw ApiException.validacion(errores);
        }

        MenuSemanal menu = new MenuSemanal();
        menu.setFechaInicio(datos.getFechaInicio());
        menu.setFechaFin(datos.getFechaInicio().plusDays(4));
        menu.setEstado(EstadoMenu.BORRADOR);
        menu.setPublicadoEn(null);
        armarDias(menu, opciones);
        return menuRepository.save(menu);
    }

    @Transactional
    public MenuSemanal actualizar(Integer id, MenuSemanal datos) {
        MenuSemanal menu = buscar(id);
        if (menu.getEstado() == EstadoMenu.PUBLICADO) {
            throw ApiException.conflicto("Un menú publicado no se puede editar");
        }
        Map<String, String> errores = new HashMap<>();
        validarSemana(datos.getFechaInicio(), menu, errores);
        List<Plato[]> opciones = resolverOpciones(datos, errores);
        if (!errores.isEmpty()) {
            throw ApiException.validacion(errores);
        }

        menu.setFechaInicio(datos.getFechaInicio());
        menu.setFechaFin(datos.getFechaInicio().plusDays(4));
        armarDias(menu, opciones);
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

        Map<String, String> errores = new HashMap<>();
        List<MenuDia> dias = menu.getDias();
        if (dias == null || dias.size() != 5) {
            errores.put("dias", "El menú debe tener los 5 días completos");
        } else {
            for (int i = 0; i < 5; i++) {
                MenuDia d = dias.get(i);
                comprobarActivo(d.getOpcion1(), i, "opcion1", errores);
                comprobarActivo(d.getOpcion2(), i, "opcion2", errores);
                if (d.getOpcion1() != null && d.getOpcion2() != null && d.getOpcion1().getIdPlato().equals(d.getOpcion2()
                        .getIdPlato())) {
                    errores.put("dias[" + i +"].opcion2", DIAS[i] + ": las dos opciones deben ser distintas");
                }
            }
        }
        if (!errores.isEmpty()) {
            throw ApiException.validacion(errores);
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

    private void validarSemana(LocalDate inicio, MenuSemanal actual, Map<String, String> errores) {
        if (inicio == null) {
            errores.put("fechaInicio", "La fecha de inicio es obligatoria");
            return;
        }
        if (inicio.getDayOfWeek() != DayOfWeek.MONDAY) {
            errores.put("fechaInicio", "La semana ya termino");
            return;
        }
        boolean cambiaSemana = actual == null || !inicio.equals(actual.getFechaInicio());
        if (cambiaSemana && (menuRepository.existsByEstadoAndFechaInicio(EstadoMenu.BORRADOR, inicio) ||
                menuRepository.existsByEstadoAndFechaInicio(EstadoMenu.PUBLICADO, inicio))) {
            errores.put("fechaInicio", "Ya existe un menú para esa semana");
        }
    }

    private List<Plato[]> resolverOpciones(MenuSemanal datos, Map<String, String> errores) {
        List<MenuDia> dias = datos.getDias();
        if (dias == null || dias.size() != 5) {
            errores.put("dias", "El menú debe tener 5 días, de lunes a viernes, en ese orden");
            return null;
        }
        List<Plato[]> resultado = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            MenuDia d = dias.get(i);
            Plato p1 = resolverPlato(d == null ? null : d.getOpcion1(), i, "opcion1", errores);
            Plato p2 = resolverPlato(d == null ? null : d.getOpcion2(), i, "opcion2", errores);
            if (p1 != null && p2 != null && p1.getIdPlato().equals(p2.getIdPlato())) {
                errores.put("dias[" + i + "].opcion2", DIAS[i] + ": las dos opciones deben ser distintas");
            }
            resultado.add(new Plato[]{p1, p2});
        }
        return resultado;
    }

    private Plato resolverPlato(Plato referencia, int dia, String campo, Map<String, String> errores) {
        String clave = "dias[" + dia + "]." + campo;
        Integer idPlato = referencia == null ? null : referencia.getIdPlato();
        if (idPlato == null) {
            errores.put(clave, DIAS[dia] + ": debe seleccionar un plato");
            return null;
        }
        Plato plato = platoRepository.findById(idPlato).orElse(null);
        if (plato == null || !Boolean.TRUE.equals(plato.getActivo())) {
            errores.put(clave, DIAS[dia] + ": el plato no existe o esta inactivo");
            return null;
        }
        return plato;
    }

    private void comprobarActivo(Plato plato, int dia, String campo, Map<String, String> errores) {
        if (plato == null || !Boolean.TRUE.equals(plato.getActivo())) {
            errores.put("dias[" + dia + "]." + campo, DIAS[dia] + ": el plato esta inactivo, elija otro");
        }
    }

    private void armarDias(MenuSemanal menu, List<Plato[]> opciones) {
        menu.getDias().clear();
        for (int i = 0; i < 5; i++) {
            MenuDia dia = new MenuDia();
            dia.setMenuSemanal(menu);
            dia.setFecha(menu.getFechaInicio().plusDays(i));
            dia.setDiaSemana(DIAS[i]);
            dia.setOpcion1(opciones.get(i)[0]);
            dia.setOpcion2(opciones.get(i)[1]);
            menu.getDias().add(dia);
        }
    }
}

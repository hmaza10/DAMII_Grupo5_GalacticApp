package com.galactica.service;

import com.galactica.entities.Zona;
import com.galactica.exception.ApiException;
import com.galactica.repository.ClienteRepository;
import com.galactica.repository.ZonaRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ZonaService {

    private final ZonaRepository zonaRepository;
    private final ClienteRepository clienteRepository;

    public ZonaService(ZonaRepository zonaRepository, ClienteRepository clienteRepository) {
        this.zonaRepository = zonaRepository;
        this.clienteRepository = clienteRepository;
    }

    public List<Zona> listarActivas() {
        return zonaRepository.findByActivoTrue();
    }

    public Page<Zona> listarPaginado(int page, int size, String texto) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 50)),
                Sort.by("idZona").ascending());
        if (texto != null && !texto.isBlank()) {
            return zonaRepository.findByNombreContainingIgnoreCase(texto.trim(), pageable);
        }
        return zonaRepository.findAll(pageable);
    }

    public Zona obtener(Integer id) {
        return zonaRepository.findById(id).orElseThrow(() -> ApiException.noEncontrado("La zona no existe"));
    }

    public List<Zona> buscarPorNombre(String texto) {
        return zonaRepository.findByNombreContainingIgnoreCaseAndActivoTrue(texto);
    }

    @Transactional
    public Zona crear(Zona zona) {
        validar(zona, null);
        zona.setIdZona(null);
        zona.setNombre(zona.getNombre().trim());
        zona.setTarifaDelivery(tarifaOCero(zona));
        zona.setActivo(true);
        return zonaRepository.save(zona);
    }

    @Transactional
    public Zona actualizar(Integer id, Zona datos) {
        validar(datos, id);
        Zona zona = obtener(id);
        zona.setNombre(datos.getNombre().trim());

        zona.setTarifaDelivery(tarifaOCero(datos));
        return zonaRepository.save(zona);
    }

    @Transactional
    public Zona desactivar(Integer id) {
        Zona zona = obtener(id);
        if (!clienteRepository.findByZonaIdZonaAndActivoTrue(id).isEmpty()) {
            throw ApiException.conflicto("No se puede desactivar: la zona tiene clientes activos");
        }
        zona.setActivo(false);
        return zonaRepository.save(zona);
    }

    @Transactional
    public Zona activar(Integer id) {
        Zona zona = obtener(id);
        zona.setActivo(true);
        return zonaRepository.save(zona);
    }

    private BigDecimal tarifaOCero(Zona zona) {
        return zona.getTarifaDelivery() == null ? BigDecimal.ZERO : zona.getTarifaDelivery();
    }

    private void validar(Zona zona, Integer idActual) {
        Map<String, String> errores = new HashMap<>();
        if (zona.getNombre() == null || zona.getNombre().isBlank()) {
            errores.put("nombre", "El nombre es obligatorio");
        } else {
            String nombre = zona.getNombre().trim();
            boolean repetido = zonaRepository.findByNombreContainingIgnoreCaseAndActivoTrue(nombre).stream()
                    .anyMatch(z -> z.getNombre().equalsIgnoreCase(nombre) && !z.getIdZona().equals(idActual));
            if (repetido) {
                errores.put("nombre", "Ya existe una zona activa con ese nombre");
            }
        }
        if (zona.getTarifaDelivery() != null && zona.getTarifaDelivery().compareTo(BigDecimal.ZERO) < 0) {
            errores.put("tarifaDelivery", "La tarifa no puede ser negativa");
        }
        if (!errores.isEmpty()) {
            throw ApiException.validacion(errores);
        }
    }
}

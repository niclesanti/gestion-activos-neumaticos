package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.service;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioApi;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.dto.ServicioReparacionRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.dto.ServicioReparacionResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.entity.ServicioReparacion;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.mapper.ServicioReparacionMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.repository.ServicioReparacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServicioReparacionServiceImpl implements ServicioReparacionService {

    static final String MENSAJE_NOMBRE_DUPLICADO = "Ya existe un servicio de reparación con el nombre %s";

    private final ServicioReparacionRepository servicioReparacionRepository;
    private final ServicioReparacionMapper servicioReparacionMapper;
    private final DomicilioApi domicilioApi;

    @Override
    @Transactional
    public ServicioReparacionResponseDTO registrarServicioReparacion(ServicioReparacionRequestDTO request,
            String usuario) {
        String nombre = request.nombre().trim();
        log.debug("Registrando servicio de reparación: usuario={}, nombre={}, conDomicilio={}", usuario, nombre,
                request.domicilio() != null);
        if (servicioReparacionRepository.existsByNombreIgnoreCase(nombre)) {
            log.warn("Alta rechazada: servicio de reparación {} duplicado (usuario {})", nombre, usuario);
            throw new EntidadDuplicadaException(MENSAJE_NOMBRE_DUPLICADO.formatted(nombre));
        }

        ServicioReparacion servicio = servicioReparacionMapper.toEntity(request);
        servicio.setNombre(nombre);
        servicio.setEmail(normalizarEmail(request.email()));
        servicio.setTelefono(opcional(request.telefono()));
        // Misma transacción: si el alta del servicio falla, el domicilio se revierte con ella.
        if (request.domicilio() != null) {
            servicio.setDomicilioId(domicilioApi.registrarDomicilio(request.domicilio(), usuario));
        }

        try {
            servicio = servicioReparacionRepository.saveAndFlush(servicio);
        } catch (DataIntegrityViolationException ex) {
            // Otra alta concurrente con el mismo nombre ganó la carrera entre la
            // verificación y el INSERT: el índice único la frena.
            log.warn("Alta rechazada por la base: servicio de reparación {} duplicado (usuario {})", nombre,
                    usuario);
            throw new EntidadDuplicadaException(MENSAJE_NOMBRE_DUPLICADO.formatted(nombre), ex);
        }
        log.info("Servicio de reparación {} registrado por {}", servicio.getPublicId(), usuario);

        DomicilioResponseDTO domicilio = servicio.getDomicilioId() == null
                ? null
                : domicilioApi.buscarDomicilio(servicio.getDomicilioId()).orElse(null);
        return servicioReparacionMapper.toResponseDTO(servicio, domicilio);
    }

    /*
    * -----------------
    * MÉTODOS PRIVADOS
    * -----------------
    * Implementación de métodos privados para la lógica interna del servicio,
    * código auxiliar, validaciones, código repetitivo.
    */

    /** Recorta los extremos; un texto opcional vacío equivale a no tenerlo. */
    private static String opcional(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    /** Recorta y pasa a minúsculas; vacío equivale a no tenerlo. */
    private static String normalizarEmail(String email) {
        String recortado = opcional(email);
        return recortado == null ? null : recortado.toLowerCase(Locale.ROOT);
    }

}

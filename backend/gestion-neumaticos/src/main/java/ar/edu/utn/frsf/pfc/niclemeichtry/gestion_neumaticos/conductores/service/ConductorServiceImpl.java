package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto.ConductorRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.dto.ConductorResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.entity.Conductor;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.mapper.ConductorMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.repository.ConductorRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConductorServiceImpl implements ConductorService {

    static final String MENSAJE_DNI_DUPLICADO = "Ya existe un conductor con el DNI %s";

    private final ConductorRepository conductorRepository;
    private final ConductorMapper conductorMapper;

    @Override
    @Transactional
    public ConductorResponseDTO registrarConductor(ConductorRequestDTO conductorRequestDTO, String usuario) {
        String dni = normalizarDni(conductorRequestDTO.dni());
        log.debug("Registrando conductor: usuario={}, dni={}", usuario, dni);
        if (dni != null && conductorRepository.existsByDni(dni)) {
            log.warn("Alta rechazada: DNI {} duplicado (usuario {})", dni, usuario);
            throw new EntidadDuplicadaException(MENSAJE_DNI_DUPLICADO.formatted(dni));
        }

        Conductor conductor = conductorMapper.toEntity(conductorRequestDTO);
        conductor.setDni(dni);
        conductor.setNombre(conductorRequestDTO.nombre().trim());
        conductor.setApellido(conductorRequestDTO.apellido().trim());

        try {
            conductor = conductorRepository.saveAndFlush(conductor);
        } catch (DataIntegrityViolationException ex) {
            // Otra alta concurrente con el mismo DNI ganó la carrera entre la
            // verificación y el INSERT: la restricción UNIQUE la frena.
            log.warn("Alta rechazada por la base: DNI {} duplicado (usuario {})", dni, usuario);
            throw new EntidadDuplicadaException(MENSAJE_DNI_DUPLICADO.formatted(dni), ex);
        }
        log.info("Conductor {} registrado por {}", conductor.getPublicId(), usuario);
        return conductorMapper.toResponseDTO(conductor);
    }

    /*
    * -----------------
    * MÉTODOS PRIVADOS
    * -----------------
    * Implementación de métodos privados para la lógica interna del servicio,
    * código auxiliar, validaciones, código repetitivo.
    */

    /** Recorta los extremos; un DNI vacío equivale a no tenerlo. */
    private static String normalizarDni(String dni) {
        if (dni == null || dni.isBlank()) {
            return null;
        }
        return dni.trim();
    }

}

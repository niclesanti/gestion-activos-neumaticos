package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.EnsambladorAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.EspecificacionesAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.FiltroAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.OrdenAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.RegistroAuditoriaDTO;
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

    private static final String MENSAJE_DNI_DUPLICADO = "Ya existe un conductor con el DNI %s";

    private final EnsambladorAuditoria ensamblador;
    private final ConductorRepository conductorRepository;
    private final ConductorMapper conductorMapper;

    @Override
    @Transactional
    public ConductorResponseDTO registrarConductor(ConductorRequestDTO conductorRequestDTO, String usuario) {
        // El mapper ya normaliza los textos (recortes; un DNI vacío = null).
        Conductor conductor = conductorMapper.toEntity(conductorRequestDTO);
        String dni = conductor.getDni();
        log.debug("Registrando conductor: usuario={}, dni={}", usuario, dni);
        if (dni != null && conductorRepository.existsByDni(dni)) {
            log.warn("Alta rechazada: DNI {} duplicado (usuario {})", dni, usuario);
            throw new EntidadDuplicadaException(MENSAJE_DNI_DUPLICADO.formatted(dni));
        }

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

    @Override
    @Transactional(readOnly = true)
    public Page<RegistroAuditoriaDTO> listarAuditoria(FiltroAuditoria filtro, Pageable pageable) {
        log.debug("Consultando auditoría de conductores: filtro={}, pageable={}", filtro, pageable);
        Specification<Conductor> especificacion = EspecificacionesAuditoria.<Conductor>de(filtro)
                .and(EspecificacionesAuditoria.contiene(filtro.q(), "nombre", "apellido", "dni"));
        Page<Conductor> pagina = conductorRepository.findAll(especificacion, OrdenAuditoria.validar(pageable));
        return ensamblador.ensamblar(pagina, Conductor::getPublicId, c -> c.getApellido() + ", " + c.getNombre());
    }

}

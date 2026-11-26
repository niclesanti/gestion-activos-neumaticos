package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
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
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.api.ConductorApi;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.api.ConductorResumenDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.NeumaticoNoDisponibleException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.PosicionOcupadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.NeumaticoApi;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.NeumaticoMontadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.NeumaticoMontajeDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.EspecificacionesUnidadTransporte;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.OrdenUnidadesTransporte;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.Patentes;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.AsignacionNeumaticoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.ConfiguracionUnidadDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteDetalleDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.ConfiguracionUnidad;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.SlotUnidadTransporte;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.UnidadTransporte;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.event.NeumaticoAsignado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.event.NeumaticoDesasignado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.mapper.UnidadTransporteMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.repository.ConfiguracionUnidadRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.repository.UnidadTransporteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UnidadTransporteServiceImpl implements UnidadTransporteService {

    static final String MENSAJE_PATENTE_DUPLICADA = "Ya existe una unidad de transporte con la patente %s";
    static final String MENSAJE_UNIDAD_INEXISTENTE = "La unidad de transporte indicada no existe";
    static final String MENSAJE_CONFIGURACION_INEXISTENTE = "La configuración de unidad indicada no existe";
    private static final String MENSAJE_POSICION_OCUPADA = "La posición %s ya tiene un neumático montado";

    private final EnsambladorAuditoria ensamblador;
    private final UnidadTransporteRepository unidadTransporteRepository;
    private final ConfiguracionUnidadRepository configuracionUnidadRepository;
    private final UnidadTransporteFactory unidadTransporteFactory;
    private final UnidadTransporteMapper unidadTransporteMapper;
    private final NeumaticoApi neumaticoApi;
    private final ConductorApi conductorApi;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UnidadTransporteResponseDTO registrarUnidadTransporte(UnidadTransporteRequestDTO unidadTransporteRequestDTO,
            String usuario) {
        String patente = Patentes.normalizar(unidadTransporteRequestDTO.patente());
        Long configuracionId = unidadTransporteRequestDTO.configuracionId();
        log.debug("Registrando unidad de transporte: usuario={}, patente={}, configuracionId={}", usuario, patente,
                configuracionId);
        if (unidadTransporteRepository.existsByPatente(patente)) {
            log.warn("Alta rechazada: unidad de transporte {} duplicada (usuario {})", patente, usuario);
            throw new EntidadDuplicadaException(MENSAJE_PATENTE_DUPLICADA.formatted(patente));
        }
        ConfiguracionUnidad configuracion = configuracionUnidadRepository.findConEjesById(configuracionId)
                .orElseThrow(() -> {
                    log.warn("Alta rechazada: la configuración {} no existe (usuario {})", configuracionId, usuario);
                    return new EntidadNoEncontradaException(MENSAJE_CONFIGURACION_INEXISTENTE);
                });

        UnidadTransporte unidad = unidadTransporteFactory.crearUnidad(patente, configuracion);
        try {
            unidad = unidadTransporteRepository.saveAndFlush(unidad);
        } catch (DataIntegrityViolationException ex) {
            // Otra alta concurrente con la misma patente ganó la carrera entre la
            // verificación y el INSERT: el índice único la frena.
            log.warn("Alta rechazada por la base: unidad de transporte {} duplicada (usuario {})", patente, usuario);
            throw new EntidadDuplicadaException(MENSAJE_PATENTE_DUPLICADA.formatted(patente), ex);
        }
        log.info("Unidad de transporte {} ({}) registrada con {} slots por {}", unidad.getPublicId(), patente,
                unidad.getSlots().size(), usuario);
        return unidadTransporteMapper.toResponseDTO(unidad);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfiguracionUnidadDTO> listarConfiguraciones() {
        List<ConfiguracionUnidadDTO> configuraciones = configuracionUnidadRepository.findAllByOrderByIdAsc().stream()
                .map(unidadTransporteMapper::toConfiguracionDTO)
                .toList();
        log.debug("Configuraciones de unidad listadas: {}", configuraciones.size());
        return configuraciones;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UnidadTransporteListadoDTO> buscar(String patente, Pageable pageable) {
        log.debug("Buscando unidades de transporte: patente={}, pageable={}", patente, pageable);
        if (!OrdenUnidadesTransporte.esValido(pageable)) {
            log.warn("Búsqueda de unidades rechazada: orden no permitido {}", pageable.getSort());
            throw new ParametroInvalidoException("Solo se puede ordenar por patente");
        }
        String termino = Patentes.normalizar(patente);
        if (termino != null && termino.isBlank()) {
            termino = null;
        }
        Specification<UnidadTransporte> especificacion = EspecificacionesUnidadTransporte.activas()
                .and(EspecificacionesUnidadTransporte.patenteContiene(termino));
        Page<UnidadTransporteListadoDTO> pagina = unidadTransporteRepository.findAll(especificacion, pageable)
                .map(unidadTransporteMapper::toListadoDTO);
        log.debug("Unidades de transporte encontradas: {}", pagina.getTotalElements());
        return pagina;
    }

    @Override
    @Transactional(readOnly = true)
    public UnidadTransporteDetalleDTO obtenerDetalle(UUID publicId) {
        log.debug("Consultando la ficha de la unidad de transporte {}", publicId);
        UnidadTransporte unidad = unidadTransporteRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> {
                    log.warn("Ficha rechazada: la unidad de transporte {} no existe o está dada de baja", publicId);
                    return new EntidadNoEncontradaException(MENSAJE_UNIDAD_INEXISTENTE);
                });
        return armarDetalle(unidad);
    }

    @Override
    @Transactional
    public UnidadTransporteDetalleDTO asignarNeumatico(UUID publicId, String codigo,
            AsignacionNeumaticoRequestDTO request, String usuario) {
        log.debug("Montando neumático: unidad={}, posicion={}, neumatico={}, conductor={}, reemplazar={}, usuario={}",
                publicId, codigo, request.neumaticoPublicId(), request.conductorPublicId(), request.aceptaReemplazo(),
                usuario);
        UnidadTransporte unidad = unidadTransporteRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> {
                    log.warn("Montaje rechazado: la unidad de transporte {} no existe o está dada de baja", publicId);
                    return new EntidadNoEncontradaException(MENSAJE_UNIDAD_INEXISTENTE);
                });
        SlotUnidadTransporte slot = unidad.buscarSlot(codigo)
                .orElseThrow(() -> {
                    log.warn("Montaje rechazado: la posición {} no es de la unidad {}", codigo, publicId);
                    return new EntidadNoEncontradaException("La posición indicada no pertenece a la unidad");
                });
        ConductorResumenDTO conductor = buscarConductor(request.conductorPublicId());
        Instant fecha = Instant.now();

        NeumaticoMontajeDTO saliente = null;
        if (slot.estaOcupado()) {
            if (!request.aceptaReemplazo()) {
                log.warn("Montaje rechazado: la posición {} de la unidad {} está ocupada y no se aceptó el reemplazo",
                        slot.getCodigo(), publicId);
                throw new PosicionOcupadaException(MENSAJE_POSICION_OCUPADA.formatted(slot.getCodigo()));
            }
            saliente = neumaticoApi.desmontar(slot.getNeumaticoActualId());
            if (saliente.publicId().equals(request.neumaticoPublicId())) {
                log.warn("Montaje rechazado: el neumático {} ya está en la posición {} de la unidad {}",
                        saliente.publicId(), slot.getCodigo(), publicId);
                throw new NeumaticoNoDisponibleException("El neumático ya está montado en esa posición");
            }
        }
        NeumaticoMontajeDTO entrante;
        try {
            entrante = neumaticoApi.montar(request.neumaticoPublicId(), slot.getId());
            slot.asignarNeumatico(entrante.id());
            // El slot es un hijo del agregado: se toca la raíz para que la unidad quede firmada (HU-43).
            unidad.registrarModificacion(fecha);
            unidadTransporteRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // Otro montaje concurrente ocupó la posición (o montó el mismo neumático) entre la lectura y la
            // escritura: los índices únicos de V20 lo frenan.
            log.warn("Montaje rechazado por la base: la posición {} de la unidad {} cambió en paralelo (usuario {})",
                    slot.getCodigo(), publicId, usuario);
            throw new PosicionOcupadaException(MENSAJE_POSICION_OCUPADA.formatted(slot.getCodigo()), ex);
        }

        if (saliente != null) {
            eventPublisher.publishEvent(new NeumaticoDesasignado(saliente.id(), saliente.numeroSerie(),
                    unidad.getId(), unidad.getPatente(), slot.getId(), slot.getCodigo(), usuario, fecha));
        }
        eventPublisher.publishEvent(new NeumaticoAsignado(entrante.id(), entrante.numeroSerie(), unidad.getId(),
                unidad.getPatente(), slot.getId(), slot.getCodigo(),
                conductor == null ? null : conductor.id(),
                conductor == null ? null : conductor.nombreCompleto(),
                usuario, fecha));
        log.info("Neumático {} montado en {} {} por {} (reemplazó a {}, conductor {})", entrante.publicId(),
                unidad.getPatente(), slot.getCodigo(), usuario, saliente == null ? null : saliente.publicId(),
                conductor == null ? null : conductor.publicId());
        return armarDetalle(unidad);
    }

    /** El conductor es opcional: null si no se indicó, error si se indicó uno inexistente o dado de baja. */
    private ConductorResumenDTO buscarConductor(UUID conductorPublicId) {
        if (conductorPublicId == null) {
            return null;
        }
        return conductorApi.buscarActivoPorPublicId(conductorPublicId)
                .orElseThrow(() -> {
                    log.warn("Montaje rechazado: el conductor {} no existe o está dado de baja", conductorPublicId);
                    return new EntidadNoEncontradaException("El conductor indicado no existe");
                });
    }

    /** Ficha de la unidad: configuración con ejes y cada posición con su neumático montado. */
    private UnidadTransporteDetalleDTO armarDetalle(UnidadTransporte unidad) {
        // Carga los ejes en el contexto de persistencia: el mapeo de la configuración los recorre.
        configuracionUnidadRepository.findConEjesById(unidad.getConfiguracion().getId());
        List<Long> idsNeumaticos = unidad.getSlots().stream()
                .map(SlotUnidadTransporte::getNeumaticoActualId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, NeumaticoMontadoDTO> neumaticos = neumaticoApi.buscarMontadosPorIds(idsNeumaticos);
        log.debug("Ficha de la unidad {} armada con {} neumáticos montados", unidad.getPublicId(), neumaticos.size());
        return unidadTransporteMapper.toDetalleDTO(unidad, neumaticos);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegistroAuditoriaDTO> listarAuditoria(FiltroAuditoria filtro, Pageable pageable) {
        log.debug("Consultando auditoría de unidades de transporte: filtro={}, pageable={}", filtro, pageable);
        Specification<UnidadTransporte> especificacion = EspecificacionesAuditoria.<UnidadTransporte>de(filtro)
                .and(EspecificacionesAuditoria.contiene(filtro.q(), "patente"));
        Page<UnidadTransporte> pagina = unidadTransporteRepository.findAll(especificacion, OrdenAuditoria.validar(pageable));
        return ensamblador.ensamblar(pagina, UnidadTransporte::getPublicId, UnidadTransporte::getPatente);
    }

}

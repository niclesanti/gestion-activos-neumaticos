package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.Patentes;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.ConfiguracionUnidadDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.ConfiguracionUnidad;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.UnidadTransporte;
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
    static final String MENSAJE_CONFIGURACION_INEXISTENTE = "La configuración de unidad indicada no existe";

    private final UnidadTransporteRepository unidadTransporteRepository;
    private final ConfiguracionUnidadRepository configuracionUnidadRepository;
    private final UnidadTransporteFactory unidadTransporteFactory;
    private final UnidadTransporteMapper unidadTransporteMapper;

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

}

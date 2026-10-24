package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioApi;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.entity.Domicilio;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.mapper.DomicilioMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.repository.DomicilioRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.repository.LocalidadRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.repository.ProvinciaRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DomicilioApiImpl implements DomicilioApi {

    static final String MENSAJE_PROVINCIA_INEXISTENTE = "La provincia indicada no existe";

    private final DomicilioRepository domicilioRepository;
    private final LocalidadRepository localidadRepository;
    private final ProvinciaRepository provinciaRepository;
    private final DomicilioMapper domicilioMapper;

    @Override
    @Transactional
    public Long registrarDomicilio(DomicilioRequestDTO domicilioRequestDTO, String usuario) {
        Long provinciaId = domicilioRequestDTO.provinciaId();
        String localidad = normalizarLocalidad(domicilioRequestDTO.localidad());
        log.debug("Registrando domicilio: usuario={}, provinciaId={}, localidad={}", usuario, provinciaId, localidad);
        if (!provinciaRepository.existsById(provinciaId)) {
            log.warn("Alta de domicilio rechazada: la provincia {} no existe (usuario {})", provinciaId, usuario);
            throw new EntidadNoEncontradaException(MENSAJE_PROVINCIA_INEXISTENTE);
        }
        Long localidadId = localidadRepository.buscarOCrear(provinciaId, localidad,
                normalizarCodigoPostal(domicilioRequestDTO.codigoPostal()));

        Domicilio domicilio = domicilioMapper.toEntity(domicilioRequestDTO);
        domicilio.setCalle(domicilioRequestDTO.calle().trim());
        domicilio.setAltura(domicilioRequestDTO.altura().trim());
        domicilio.setPiso(opcional(domicilioRequestDTO.piso()));
        domicilio.setDepartamento(opcional(domicilioRequestDTO.departamento()));
        domicilio.setLocalidad(localidadRepository.getReferenceById(localidadId));
        domicilio = domicilioRepository.saveAndFlush(domicilio);

        log.info("Domicilio {} registrado por {} (localidad {})", domicilio.getId(), usuario, localidadId);
        return domicilio.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DomicilioResponseDTO> buscarDomicilio(Long id) {
        log.debug("Buscando domicilio {}", id);
        return domicilioRepository.findWithLocalidadById(id).map(domicilioMapper::toResponseDTO);
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

    /** Recorta y deja un solo espacio entre palabras, para no duplicar localidades. */
    private static String normalizarLocalidad(String localidad) {
        return localidad.trim().replaceAll("\\s+", " ");
    }

    /** Recorta y pasa a mayúsculas (CPA); vacío equivale a no tenerlo. */
    private static String normalizarCodigoPostal(String codigoPostal) {
        String recortado = opcional(codigoPostal);
        return recortado == null ? null : recortado.toUpperCase(Locale.ROOT);
    }

}

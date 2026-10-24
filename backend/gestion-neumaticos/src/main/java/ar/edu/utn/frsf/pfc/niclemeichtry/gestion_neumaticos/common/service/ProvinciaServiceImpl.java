package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.dto.ProvinciaResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.mapper.DomicilioMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.repository.ProvinciaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProvinciaServiceImpl implements ProvinciaService {

    private final ProvinciaRepository provinciaRepository;
    private final DomicilioMapper domicilioMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProvinciaResponseDTO> listarProvincias() {
        List<ProvinciaResponseDTO> provincias = provinciaRepository.findAllByOrderByNombreAsc().stream()
                .map(domicilioMapper::toProvinciaDTO)
                .toList();
        log.debug("Provincias listadas: {}", provincias.size());
        return provincias;
    }

}

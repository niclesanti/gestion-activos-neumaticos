package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.service;

import java.util.List;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.dto.ProvinciaResponseDTO;

public interface ProvinciaService {

    /** Todas las provincias, ordenadas por nombre. */
    List<ProvinciaResponseDTO> listarProvincias();

}

package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service;

import java.util.List;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.ConfiguracionUnidadDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteResponseDTO;

public interface UnidadTransporteService {

    /**
     * Registra una unidad de transporte y genera todos los slots de su
     * configuración, sin neumático asignado. La patente se guarda normalizada
     * (mayúsculas, sin espacios ni guiones).
     *
     * @param unidadTransporteRequestDTO datos del alta, ya validados con Bean Validation.
     * @param usuario                    identificador público del usuario que hace el alta.
     * @return la unidad registrada, con sus slots.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException
     *         si ya existe una unidad con la misma patente.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException
     *         si la configuración no existe.
     */
    UnidadTransporteResponseDTO registrarUnidadTransporte(UnidadTransporteRequestDTO unidadTransporteRequestDTO,
            String usuario);

    /** Las configuraciones precargadas, con sus ejes en orden. */
    List<ConfiguracionUnidadDTO> listarConfiguraciones();

}

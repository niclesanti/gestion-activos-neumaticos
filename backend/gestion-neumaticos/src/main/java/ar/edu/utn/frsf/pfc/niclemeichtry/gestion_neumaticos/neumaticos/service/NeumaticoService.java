package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.service;

import java.util.List;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.MarcaNeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoResponseDTO;

public interface NeumaticoService {

    /**
     * Registra un neumático (HU-11). Queda {@code EN_ESPERA} de que se opere con
     * él, con su tipo de eje actual tal como lo indicó el usuario. El número de
     * serie se guarda en mayúsculas y sin espacios en los extremos (vacío = sin
     * número de serie). Si el estado es {@code NUEVO}, publica el evento de
     * compra {@code NeumaticoComprado}, que trazabilidad registra de forma
     * asíncrona.
     *
     * @param neumaticoRequestDTO datos del alta, ya validados con Bean Validation.
     * @param usuario             identificador público del usuario que hace el alta.
     * @return el neumático registrado.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadDuplicadaException
     *         si ya existe un neumático con el mismo número de serie.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException
     *         si la marca, o el proveedor de un neumático nuevo, no existen.
     */
    NeumaticoResponseDTO registrarNeumatico(NeumaticoRequestDTO neumaticoRequestDTO, String usuario);

    /** Marcas de neumáticos disponibles, ordenadas por nombre. */
    List<MarcaNeumaticoResponseDTO> listarMarcas();

}

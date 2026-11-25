package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.FiltroAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.RegistroAuditoriaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.MarcaNeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoDetalleDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoListadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto.NeumaticoResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.EstadoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.SituacionOperativa;

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

    /**
     * Busca neumáticos activos (HU-28), paginados. Los filtros son opcionales y se combinan con AND: el
     * número de serie coincide por contenido y sin distinguir mayúsculas; marca, estado y situación
     * operativa por igualdad. Solo se ordena por número de serie.
     *
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException si se ordena por otro campo.
     */
    Page<NeumaticoListadoDTO> buscar(String numeroSerie, Long marcaId, EstadoNeumatico estado,
            SituacionOperativa situacion, Pageable pageable);

    /**
     * Ficha de un neumático (HU-14): datos técnicos, situación vigente y ubicación (unidad y posición,
     * centro o servicio, según la situación; en espera o de baja no tiene). Un neumático de baja
     * operativa se devuelve; uno con baja lógica se comporta como inexistente.
     *
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException
     *         si no existe o tiene baja lógica.
     */
    NeumaticoDetalleDTO obtenerDetalle(UUID publicId);

    /** Marcas de neumáticos disponibles, ordenadas por nombre. */
    List<MarcaNeumaticoResponseDTO> listarMarcas();

    /**
     * Fichas de auditoría (alta, última modificación y baja) de neumáticos, paginadas y
     * filtradas. Solo para administradores; el orden se limita a los campos de auditoría.
     *
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException si se ordena por otro campo.
     */
    Page<RegistroAuditoriaDTO> listarAuditoria(FiltroAuditoria filtro, Pageable pageable);

}

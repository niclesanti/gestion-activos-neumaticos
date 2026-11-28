package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.FiltroAuditoria;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.RegistroAuditoriaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.AsignacionNeumaticoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.ConfiguracionUnidadDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteDetalleDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteListadoDTO;
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

    /**
     * Busca las unidades activas cuya patente contiene el término (sin distinguir mayúsculas, espacios ni
     * guiones). Sin término devuelve todas las activas. Paginada; solo se ordena por patente.
     *
     * @param patente  término de búsqueda, opcional.
     * @param pageable página pedida.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException si se ordena por otro campo.
     */
    Page<UnidadTransporteListadoDTO> buscar(String patente, Pageable pageable);

    /**
     * Ficha de una unidad activa: configuración, posiciones y el neumático montado en cada una.
     *
     * @param publicId identificador público de la unidad.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException
     *         si no existe o está dada de baja.
     */
    UnidadTransporteDetalleDTO obtenerDetalle(UUID publicId);

    /**
     * Monta un neumático disponible (en stock o en espera) en una posición de la unidad (HU-31), en una
     * sola transacción: si la posición está ocupada y se aceptó el reemplazo, desmonta el actual (queda en
     * espera); monta el nuevo (sale del stock de su centro), actualiza la posición, firma la unidad y
     * publica los eventos de desasignación (si hubo) y de asignación para trazabilidad.
     *
     * @param publicId  identificador público de la unidad.
     * @param codigo    código de la posición (ej. "E2-IZQ-EXT").
     * @param request   neumático, conductor opcional y aceptación del reemplazo.
     * @param usuario   identificador público del usuario que monta.
     * @return la ficha de la unidad actualizada.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException
     *         si la unidad no existe o está dada de baja, la posición no es de la unidad, o el neumático o el
     *         conductor no existen (o el conductor está dado de baja).
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.PosicionOcupadaException
     *         si la posición está ocupada y no se aceptó el reemplazo, o la ocupó otro montaje concurrente.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.NeumaticoNoDisponibleException
     *         si el neumático no está en stock ni en espera, está dado de baja o ya está en esa posición.
     */
    UnidadTransporteDetalleDTO asignarNeumatico(UUID publicId, String codigo, AsignacionNeumaticoRequestDTO request,
            String usuario);

    /**
     * Desmonta el neumático de una posición de la unidad (HU-32), en una sola transacción y con la unidad
     * bloqueada: el neumático queda en espera, la posición libre, la unidad firmada y se publica el evento
     * de desasignación para trazabilidad.
     *
     * @param publicId identificador público de la unidad.
     * @param codigo   código de la posición (ej. "E2-IZQ-EXT").
     * @param usuario  identificador público del usuario que desmonta.
     * @return la ficha de la unidad actualizada.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException
     *         si la unidad no existe o está dada de baja, o la posición no es de la unidad.
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.PosicionLibreException
     *         si la posición no tiene neumático (o la liberó otro desmontaje concurrente).
     */
    UnidadTransporteDetalleDTO desasignarNeumatico(UUID publicId, String codigo, String usuario);

    /** Las configuraciones precargadas, con sus ejes en orden. */
    List<ConfiguracionUnidadDTO> listarConfiguraciones();

    /**
     * Fichas de auditoría (alta, última modificación y baja) de unidades de transporte, paginadas y
     * filtradas. Solo para administradores; el orden se limita a los campos de auditoría.
     *
     * @throws ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException si se ordena por otro campo.
     */
    Page<RegistroAuditoriaDTO> listarAuditoria(FiltroAuditoria filtro, Pageable pageable);

}

package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.ConfiguracionUnidad;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.UnidadTransporte;

/**
 * Fábrica de unidades de transporte (patrón Factory). El servicio depende de
 * esta interfaz y recibe la implementación por inyección de dependencias.
 */
public interface UnidadTransporteFactory {

    /**
     * Crea una unidad (sin persistir) con todos los slots que describe la
     * configuración, sin neumático asignado.
     *
     * @param patente patente ya normalizada.
     * @param config  configuración con sus ejes cargados.
     */
    UnidadTransporte crearUnidad(String patente, ConfiguracionUnidad config);

}

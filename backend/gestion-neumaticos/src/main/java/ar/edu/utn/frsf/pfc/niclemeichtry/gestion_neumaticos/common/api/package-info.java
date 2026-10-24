/**
 * API pública del módulo common para el resto de los módulos: la fachada
 * {@link ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api.DomicilioApi}
 * y los DTOs que entran y salen por ella (otros módulos los anidan en sus
 * propios DTOs, ej. el domicilio de un proveedor). Es una interfaz nombrada de
 * Modulith y lo único de common que otros módulos pueden usar.
 */
@NamedInterface("api")
package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.api;

import org.springframework.modulith.NamedInterface;

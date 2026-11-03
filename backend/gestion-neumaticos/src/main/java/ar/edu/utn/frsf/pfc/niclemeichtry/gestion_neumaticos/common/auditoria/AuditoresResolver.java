package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Puerto hacia el módulo {@code usuarios} (lo implementa {@code AuditoresResolverImpl}):
 * traduce los {@code publicId} guardados en las columnas de auditoría a datos
 * legibles. Resuelve todo en una sola consulta.
 */
public interface AuditoresResolver {

    /** Solo incluye a los usuarios encontrados. */
    Map<UUID, AuditorDTO> resolver(Collection<UUID> publicIds);
}

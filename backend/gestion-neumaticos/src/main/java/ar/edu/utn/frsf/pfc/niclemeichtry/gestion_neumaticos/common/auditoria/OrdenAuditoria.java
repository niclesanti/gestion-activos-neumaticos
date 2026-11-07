package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria;

import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.ParametroInvalidoException;

/** Lista blanca de campos por los que se puede ordenar la consulta de auditoría. */
public final class OrdenAuditoria {

    private static final Set<String> CAMPOS_PERMITIDOS = Set.of("createdAt", "updatedAt", "deletedAt");

    private OrdenAuditoria() {
    }

    /** Devuelve el mismo {@code Pageable} o falla con 400 si ordena por otro campo. */
    public static Pageable validar(Pageable pageable) {
        for (Sort.Order orden : pageable.getSort()) {
            if (!CAMPOS_PERMITIDOS.contains(orden.getProperty())) {
                throw new ParametroInvalidoException(
                        "Solo se puede ordenar por " + String.join(", ", CAMPOS_PERMITIDOS.stream().sorted().toList()));
            }
        }
        return pageable;
    }
}

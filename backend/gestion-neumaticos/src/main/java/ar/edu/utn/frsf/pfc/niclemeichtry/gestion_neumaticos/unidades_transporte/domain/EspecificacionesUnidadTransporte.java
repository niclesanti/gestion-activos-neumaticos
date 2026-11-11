package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain;

import org.springframework.data.jpa.domain.Specification;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.UnidadTransporte;

/** Filtros de la búsqueda de unidades de transporte. */
public final class EspecificacionesUnidadTransporte {

    private EspecificacionesUnidadTransporte() {
    }

    /** Unidades no dadas de baja ({@code deleted_at IS NULL}). */
    public static Specification<UnidadTransporte> activas() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    /**
     * Patente que contiene el término, ya normalizado (mayúsculas, sin espacios ni guiones). Los
     * comodines de LIKE se escapan: se busca literal. Null no filtra.
     */
    public static Specification<UnidadTransporte> patenteContiene(String patenteNormalizada) {
        if (patenteNormalizada == null) {
            return Specification.unrestricted();
        }
        String patron = "%" + patenteNormalizada
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> cb.like(root.get("patente"), patron, '\\');
    }
}

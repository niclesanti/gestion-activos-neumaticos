package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

/**
 * Filtros de la consulta de auditoría sobre las columnas que comparten todas las
 * entidades auditadas. Cada módulo los combina con su propia búsqueda de texto.
 */
public final class EspecificacionesAuditoria {

    private EspecificacionesAuditoria() {
    }

    /** Estado (activos / dados de baja) y rango de fechas de última modificación. */
    public static <T extends AuditableEntity> Specification<T> de(FiltroAuditoria filtro) {
        return Specification.<T>unrestricted()
                .and(estado(filtro.estado()))
                .and(modificadoDesde(filtro.desde()))
                .and(modificadoHasta(filtro.hasta()));
    }

    /**
     * Búsqueda de texto: contiene {@code q} (sin distinguir mayúsculas) en alguno de
     * los atributos. Los comodines de LIKE del texto se escapan: se busca literal.
     */
    public static <T extends AuditableEntity> Specification<T> contiene(String q, String... atributos) {
        if (q == null) {
            return Specification.unrestricted();
        }
        String patron = "%" + q.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> cb.or(Arrays.stream(atributos)
                .map(atributo -> cb.like(cb.lower(root.<String>get(atributo)), patron, '\\'))
                .toArray(Predicate[]::new));
    }

    static <T extends AuditableEntity> Specification<T> estado(FiltroAuditoria.EstadoFiltro estado) {
        return switch (estado) {
            case ACTIVOS -> (root, query, cb) -> cb.isNull(root.get("deletedAt"));
            case BAJAS -> (root, query, cb) -> cb.isNotNull(root.get("deletedAt"));
            case TODOS -> Specification.unrestricted();
        };
    }

    static <T extends AuditableEntity> Specification<T> modificadoDesde(LocalDate desde) {
        if (desde == null) {
            return Specification.unrestricted();
        }
        Instant inicio = desde.atStartOfDay(ZoneOffset.UTC).toInstant();
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<Instant>get("updatedAt"), inicio);
    }

    static <T extends AuditableEntity> Specification<T> modificadoHasta(LocalDate hasta) {
        if (hasta == null) {
            return Specification.unrestricted();
        }
        Instant fin = hasta.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return (root, query, cb) -> cb.lessThan(root.<Instant>get("updatedAt"), fin);
    }
}

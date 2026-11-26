package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain;

import java.util.Collection;

import org.springframework.data.jpa.domain.Specification;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.EstadoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.Neumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.SituacionOperativa;

/** Filtros de la búsqueda de neumáticos. */
public final class EspecificacionesNeumatico {

    private EspecificacionesNeumatico() {
    }

    /** Neumáticos sin baja lógica y con estado y situación operativa distintos de {@code DE_BAJA}. */
    public static Specification<Neumatico> activos() {
        return (root, query, cb) -> cb.and(
                cb.isNull(root.get("deletedAt")),
                cb.notEqual(root.get("estadoActual"), EstadoNeumatico.DE_BAJA),
                cb.notEqual(root.get("situacionOperativaVigente"), SituacionOperativa.DE_BAJA));
    }

    /**
     * Número de serie que contiene el término, ya normalizado (mayúsculas). Los comodines de LIKE se
     * escapan: se busca literal. Null no filtra.
     */
    public static Specification<Neumatico> numeroSerieContiene(String terminoNormalizado) {
        if (terminoNormalizado == null) {
            return Specification.unrestricted();
        }
        String patron = "%" + terminoNormalizado
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return (root, query, cb) -> cb.like(root.get("numeroSerie"), patron, '\\');
    }

    /** Neumáticos de la marca con ese id. Null no filtra. */
    public static Specification<Neumatico> deMarca(Long marcaId) {
        if (marcaId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("marca").get("id"), marcaId);
    }

    /** Neumáticos con ese estado técnico. Null no filtra. */
    public static Specification<Neumatico> conEstado(EstadoNeumatico estado) {
        if (estado == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("estadoActual"), estado);
    }

    /** Neumáticos con alguna de esas situaciones operativas vigentes (OR). Null o vacía no filtra. */
    public static Specification<Neumatico> conSituaciones(Collection<SituacionOperativa> situaciones) {
        if (situaciones == null || situaciones.isEmpty()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> root.get("situacionOperativaVigente").in(situaciones);
    }

    /** Neumáticos guardados en el centro de almacenamiento con ese id interno. Null no filtra. */
    public static Specification<Neumatico> enCentro(Long centroAlmacenamientoId) {
        if (centroAlmacenamientoId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("centroAlmacenamientoId"), centroAlmacenamientoId);
    }
}

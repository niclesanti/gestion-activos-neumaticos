package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain;

import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Lista blanca de campos por los que se puede ordenar la búsqueda de neumáticos. */
public final class OrdenNeumaticos {

    private static final Set<String> CAMPOS_PERMITIDOS = Set.of("numeroSerie");

    private OrdenNeumaticos() {
    }

    /** Si el pageable no ordena, o solo ordena por campos permitidos. */
    public static boolean esValido(Pageable pageable) {
        for (Sort.Order orden : pageable.getSort()) {
            if (!CAMPOS_PERMITIDOS.contains(orden.getProperty())) {
                return false;
            }
        }
        return true;
    }
}

package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain;

import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Lista blanca de campos por los que se puede ordenar la búsqueda de unidades de transporte. */
public final class OrdenUnidadesTransporte {

    private static final Set<String> CAMPOS_PERMITIDOS = Set.of("patente");

    private OrdenUnidadesTransporte() {
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

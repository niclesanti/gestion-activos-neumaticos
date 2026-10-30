package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service;

import java.util.Comparator;

import org.springframework.stereotype.Component;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.ConfiguracionUnidad;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.EjeConfiguracion;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.Lado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.PosicionRueda;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.SlotUnidadTransporte;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoRodado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoSlot;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.UnidadTransporte;

/**
 * Recorre los ejes de la configuración por orden y genera sus slots de
 * izquierda a derecha, como se ven en el esquema de ejes:
 * <ul>
 * <li>SIMPLE: IZQ-SIMPLE, DER-SIMPLE</li>
 * <li>DUAL: IZQ-EXT, IZQ-INT, DER-INT, DER-EXT</li>
 * </ul>
 * El número de eje es su orden y el tipo de eje se copia. Después agrega los
 * slots de auxilio (1..n).
 */
@Component
public class UnidadTransporteFactoryImpl implements UnidadTransporteFactory {

    @Override
    public UnidadTransporte crearUnidad(String patente, ConfiguracionUnidad config) {
        UnidadTransporte unidad = UnidadTransporte.builder()
                .patente(patente)
                .configuracion(config)
                .build();

        config.getEjes().stream()
                .sorted(Comparator.comparing(EjeConfiguracion::getOrden))
                .forEach(eje -> agregarSlotsDeEje(unidad, eje));

        for (int nroAuxilio = 1; nroAuxilio <= config.getCantSlotsAuxilio(); nroAuxilio++) {
            unidad.addSlot(SlotUnidadTransporte.builder()
                    .tipoSlot(TipoSlot.AUXILIO)
                    .nroAuxilio(nroAuxilio)
                    .build());
        }
        return unidad;
    }

    /*
    * -----------------
    * MÉTODOS PRIVADOS
    * -----------------
    */

    private static void agregarSlotsDeEje(UnidadTransporte unidad, EjeConfiguracion eje) {
        if (eje.getRodado() == TipoRodado.DUAL) {
            unidad.addSlot(slotDeEje(eje, Lado.IZQUIERDO, PosicionRueda.EXTERIOR));
            unidad.addSlot(slotDeEje(eje, Lado.IZQUIERDO, PosicionRueda.INTERIOR));
            unidad.addSlot(slotDeEje(eje, Lado.DERECHO, PosicionRueda.INTERIOR));
            unidad.addSlot(slotDeEje(eje, Lado.DERECHO, PosicionRueda.EXTERIOR));
        } else {
            unidad.addSlot(slotDeEje(eje, Lado.IZQUIERDO, PosicionRueda.SIMPLE));
            unidad.addSlot(slotDeEje(eje, Lado.DERECHO, PosicionRueda.SIMPLE));
        }
    }

    private static SlotUnidadTransporte slotDeEje(EjeConfiguracion eje, Lado lado, PosicionRueda posicionRueda) {
        return SlotUnidadTransporte.builder()
                .tipoSlot(TipoSlot.EJE)
                .nroEje(eje.getOrden())
                .tipoEje(eje.getTipoEje())
                .lado(lado)
                .posicionRueda(posicionRueda)
                .build();
    }

}

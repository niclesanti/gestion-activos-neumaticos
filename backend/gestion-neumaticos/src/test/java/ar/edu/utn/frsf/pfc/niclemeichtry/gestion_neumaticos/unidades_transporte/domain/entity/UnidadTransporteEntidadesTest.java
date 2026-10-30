package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.eje;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.semirremolque;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.tractorComun;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.tractorPesado;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Reglas de dominio de las entidades del módulo, sin persistencia. */
class UnidadTransporteEntidadesTest {

    private static SlotUnidadTransporte slotDeEje(int nroEje, Lado lado, PosicionRueda posicion) {
        return SlotUnidadTransporte.builder().tipoSlot(TipoSlot.EJE).nroEje(nroEje).tipoEje(TipoEje.TRACCION)
                .lado(lado).posicionRueda(posicion).build();
    }

    private static SlotUnidadTransporte auxilio(int nro) {
        return SlotUnidadTransporte.builder().tipoSlot(TipoSlot.AUXILIO).nroAuxilio(nro).build();
    }

    @Nested
    @DisplayName("ConfiguracionUnidad y EjeConfiguracion")
    class Configuraciones {

        @Test
        @DisplayName("un eje simple lleva 2 neumáticos y uno dual 4")
        void neumaticosPorEje() {
            assertThat(eje(1, TipoEje.DIRECCION, TipoRodado.SIMPLE).cantidadNeumaticos()).isEqualTo(2);
            assertThat(eje(1, TipoEje.ARRASTRE, TipoRodado.DUAL).cantidadNeumaticos()).isEqualTo(4);
        }

        @Test
        @DisplayName("totales de las tres configuraciones precargadas: 7, 11 y 13 slots")
        void totales() {
            assertThat(tractorComun().obtenerTotalEjes()).isEqualTo(2);
            assertThat(tractorComun().obtenerTotalSlots()).isEqualTo(7);
            assertThat(tractorPesado().obtenerTotalEjes()).isEqualTo(3);
            assertThat(tractorPesado().obtenerTotalSlots()).isEqualTo(11);
            assertThat(semirremolque().obtenerTotalEjes()).isEqualTo(3);
            assertThat(semirremolque().obtenerTotalSlots()).isEqualTo(13);
        }

        @Test
        @DisplayName("toString no recorre las relaciones (evita ciclos y cargas perezosas)")
        void toStringSinRelaciones() {
            ConfiguracionUnidad configuracion = tractorComun();

            assertThat(configuracion.toString()).contains("Tractor común").doesNotContain("ejes=");
            assertThat(configuracion.getEjes().get(0).toString()).doesNotContain("configuracion=");
        }

    }

    @Nested
    @DisplayName("SlotUnidadTransporte")
    class Slots {

        @Test
        @DisplayName("el código se deriva de la posición")
        void codigo() {
            assertThat(slotDeEje(1, Lado.IZQUIERDO, PosicionRueda.SIMPLE).getCodigo()).isEqualTo("E1-IZQ");
            assertThat(slotDeEje(1, Lado.DERECHO, PosicionRueda.SIMPLE).getCodigo()).isEqualTo("E1-DER");
            assertThat(slotDeEje(2, Lado.IZQUIERDO, PosicionRueda.EXTERIOR).getCodigo()).isEqualTo("E2-IZQ-EXT");
            assertThat(slotDeEje(2, Lado.DERECHO, PosicionRueda.INTERIOR).getCodigo()).isEqualTo("E2-DER-INT");
            assertThat(auxilio(1).getCodigo()).isEqualTo("AUX-1");
        }

        @Test
        @DisplayName("está ocupado solo con un neumático asignado")
        void ocupado() {
            SlotUnidadTransporte slot = auxilio(1);
            assertThat(slot.estaOcupado()).isFalse();

            slot.setNeumaticoActualId(10L);

            assertThat(slot.estaOcupado()).isTrue();
            assertThat(slot.toString()).doesNotContain("unidad=");
        }

    }

    @Nested
    @DisplayName("UnidadTransporte")
    class Unidades {

        @Test
        @DisplayName("addSlot enlaza el slot; los slots se filtran por eje y por auxilio")
        void slots() {
            UnidadTransporte unidad = UnidadTransporte.builder().patente("AB123CD").build();
            SlotUnidadTransporte e1 = slotDeEje(1, Lado.IZQUIERDO, PosicionRueda.SIMPLE);
            SlotUnidadTransporte e2 = slotDeEje(2, Lado.DERECHO, PosicionRueda.EXTERIOR);
            SlotUnidadTransporte aux = auxilio(1);

            unidad.addSlot(e1);
            unidad.addSlot(e2);
            unidad.addSlot(aux);

            assertThat(e1.getUnidad()).isSameAs(unidad);
            assertThat(unidad.getSlotsDeEje(1)).containsExactly(e1);
            assertThat(unidad.getSlotsDeEje(2)).containsExactly(e2);
            assertThat(unidad.getSlotsDeEje(3)).isEmpty();
            assertThat(unidad.getSlotsAuxilio()).containsExactly(aux);
            assertThat(unidad.toString()).contains("AB123CD").doesNotContain("slots=");
        }

    }

}

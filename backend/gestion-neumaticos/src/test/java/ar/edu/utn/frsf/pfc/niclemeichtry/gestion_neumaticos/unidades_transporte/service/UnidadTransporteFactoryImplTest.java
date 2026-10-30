package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PATENTE;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.configuracion;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.eje;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.semirremolque;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.tractorComun;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.tractorPesado;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.ConfiguracionUnidad;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.SlotUnidadTransporte;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoEje;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoRodado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoUnidad;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.UnidadTransporte;

class UnidadTransporteFactoryImplTest {

    private final UnidadTransporteFactory factory = new UnidadTransporteFactoryImpl();

    private static final List<String> EJE_DUAL_2 = List.of("E2-IZQ-EXT", "E2-IZQ-INT", "E2-DER-INT", "E2-DER-EXT");

    static Stream<Arguments> configuraciones() {
        return Stream.of(
                Arguments.of(tractorComun(), Stream.of(
                        List.of("E1-IZQ", "E1-DER"), EJE_DUAL_2, List.of("AUX-1"))
                        .flatMap(List::stream).toList()),
                Arguments.of(tractorPesado(), Stream.of(
                        List.of("E1-IZQ", "E1-DER"), EJE_DUAL_2,
                        List.of("E3-IZQ-EXT", "E3-IZQ-INT", "E3-DER-INT", "E3-DER-EXT"), List.of("AUX-1"))
                        .flatMap(List::stream).toList()),
                Arguments.of(semirremolque(), Stream.of(
                        List.of("E1-IZQ-EXT", "E1-IZQ-INT", "E1-DER-INT", "E1-DER-EXT"), EJE_DUAL_2,
                        List.of("E3-IZQ-EXT", "E3-IZQ-INT", "E3-DER-INT", "E3-DER-EXT"), List.of("AUX-1"))
                        .flatMap(List::stream).toList()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("configuraciones")
    @DisplayName("genera los slots de eje de izquierda a derecha y después los de auxilio, todos libres")
    void generaLosSlotsDeLaConfiguracion(ConfiguracionUnidad config, List<String> codigos) {
        UnidadTransporte unidad = factory.crearUnidad(PATENTE, config);

        assertThat(unidad.getPatente()).isEqualTo(PATENTE);
        assertThat(unidad.getConfiguracion()).isSameAs(config);
        assertThat(unidad.getSlots()).extracting(SlotUnidadTransporte::getCodigo).containsExactlyElementsOf(codigos);
        assertThat(unidad.getSlots()).hasSize(config.obtenerTotalSlots());
        assertThat(unidad.getSlots()).allSatisfy(slot -> {
            assertThat(slot.getUnidad()).isSameAs(unidad);
            assertThat(slot.estaOcupado()).isFalse();
        });
        assertThat(unidad.getId()).isNull();
        assertThat(unidad.getPublicId()).isNull();
    }

    @Test
    @DisplayName("el slot de eje copia el número y el tipo del eje; el de auxilio no tiene datos de eje")
    void copiaLosDatosDelEje() {
        UnidadTransporte unidad = factory.crearUnidad(PATENTE, tractorComun());

        assertThat(unidad.getSlotsDeEje(2)).hasSize(4).allSatisfy(slot -> {
            assertThat(slot.getNroEje()).isEqualTo(2);
            assertThat(slot.getTipoEje()).isEqualTo(TipoEje.TRACCION);
            assertThat(slot.getNroAuxilio()).isNull();
        });
        assertThat(unidad.getSlotsAuxilio()).singleElement().satisfies(slot -> {
            assertThat(slot.getNroAuxilio()).isEqualTo(1);
            assertThat(slot.getNroEje()).isNull();
            assertThat(slot.getTipoEje()).isNull();
            assertThat(slot.getLado()).isNull();
            assertThat(slot.getPosicionRueda()).isNull();
        });
    }

    @Test
    @DisplayName("recorre los ejes por orden aunque vengan desordenados y numera varios auxilios")
    void ordenaEjesYNumeraAuxilios() {
        ConfiguracionUnidad config = configuracion(9L, "Prueba", TipoUnidad.TRACTOR, 2,
                eje(2, TipoEje.TRACCION, TipoRodado.SIMPLE), eje(1, TipoEje.DIRECCION, TipoRodado.SIMPLE));

        UnidadTransporte unidad = factory.crearUnidad(PATENTE, config);

        assertThat(unidad.getSlots()).extracting(SlotUnidadTransporte::getCodigo)
                .containsExactly("E1-IZQ", "E1-DER", "E2-IZQ", "E2-DER", "AUX-1", "AUX-2");
    }

    @Test
    @DisplayName("sin auxilios solo genera los slots de eje")
    void sinAuxilios() {
        ConfiguracionUnidad config = configuracion(9L, "Prueba", TipoUnidad.SEMIRREMOLQUE, 0,
                eje(1, TipoEje.ARRASTRE, TipoRodado.DUAL));

        UnidadTransporte unidad = factory.crearUnidad(PATENTE, config);

        assertThat(unidad.getSlots()).hasSize(4);
        assertThat(unidad.getSlotsAuxilio()).isEmpty();
    }

}

package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.mapper;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PATENTE;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.UNIDAD_PUBLIC_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.tractorComun;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.ConfiguracionUnidadDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.ConfiguracionUnidadResumenDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.EjeConfiguracionDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.SlotUnidadTransporteDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.dto.UnidadTransporteResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.ConfiguracionUnidad;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.Lado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.PosicionRueda;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoEje;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoRodado;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoSlot;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.TipoUnidad;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.UnidadTransporte;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service.UnidadTransporteFactoryImpl;

/** Prueba la implementación que genera MapStruct (sin contexto de Spring: no tiene dependencias). */
class UnidadTransporteMapperTest {

    private final UnidadTransporteMapper mapper = new UnidadTransporteMapperImpl();

    @Test
    void toResponseDTOResumeLaConfiguracionYDerivaLosCodigos() {
        UnidadTransporte unidad = new UnidadTransporteFactoryImpl().crearUnidad(PATENTE, tractorComun());
        unidad.setPublicId(UNIDAD_PUBLIC_ID);
        unidad.getSlots().get(0).setNeumaticoActualId(10L);

        UnidadTransporteResponseDTO dto = mapper.toResponseDTO(unidad);

        assertThat(dto.publicId()).isEqualTo(UNIDAD_PUBLIC_ID);
        assertThat(dto.patente()).isEqualTo(PATENTE);
        assertThat(dto.configuracion())
                .isEqualTo(new ConfiguracionUnidadResumenDTO(1L, "Tractor común", TipoUnidad.TRACTOR));
        assertThat(dto.slots()).hasSize(7);
        assertThat(dto.slots().get(0)).isEqualTo(new SlotUnidadTransporteDTO("E1-IZQ", TipoSlot.EJE, 1,
                TipoEje.DIRECCION, Lado.IZQUIERDO, PosicionRueda.SIMPLE, null, true));
        assertThat(dto.slots().get(6)).isEqualTo(new SlotUnidadTransporteDTO("AUX-1", TipoSlot.AUXILIO, null,
                null, null, null, 1, false));
    }

    @Test
    void toConfiguracionDTOIncluyeEjesYTotales() {
        ConfiguracionUnidadDTO dto = mapper.toConfiguracionDTO(tractorComun());

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.nombre()).isEqualTo("Tractor común");
        assertThat(dto.tipoUnidad()).isEqualTo(TipoUnidad.TRACTOR);
        assertThat(dto.cantSlotsAuxilio()).isEqualTo(1);
        assertThat(dto.totalEjes()).isEqualTo(2);
        assertThat(dto.totalSlots()).isEqualTo(7);
        assertThat(dto.ejes()).containsExactly(
                new EjeConfiguracionDTO(1, TipoEje.DIRECCION, TipoRodado.SIMPLE, 2),
                new EjeConfiguracionDTO(2, TipoEje.TRACCION, TipoRodado.DUAL, 4));
    }

    @Test
    void nullDevuelveNull() {
        assertThat(mapper.toResponseDTO(null)).isNull();
        assertThat(mapper.toSlotDTO(null)).isNull();
        assertThat(mapper.toConfiguracionDTO(null)).isNull();
        assertThat(mapper.toEjeDTO(null)).isNull();
    }

    @Test
    void camposNulosQuedanNulos() {
        UnidadTransporteResponseDTO unidad = mapper.toResponseDTO(new UnidadTransporte());
        assertThat(unidad.publicId()).isNull();
        assertThat(unidad.patente()).isNull();
        assertThat(unidad.configuracion()).isNull();
        assertThat(unidad.slots()).isEmpty();

        ConfiguracionUnidadDTO configuracion = mapper.toConfiguracionDTO(
                ConfiguracionUnidad.builder().cantSlotsAuxilio(0).build());
        assertThat(configuracion.nombre()).isNull();
        assertThat(configuracion.tipoUnidad()).isNull();
        assertThat(configuracion.ejes()).isEmpty();
        assertThat(configuracion.totalSlots()).isZero();
    }

}

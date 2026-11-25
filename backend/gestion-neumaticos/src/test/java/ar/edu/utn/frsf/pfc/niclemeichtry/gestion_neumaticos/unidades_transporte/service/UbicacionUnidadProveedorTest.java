package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PATENTE;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.SLOT_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.unidadConNeumaticoMontado;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.TipoUbicacion;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.UbicacionOperativaDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain.entity.SlotUnidadTransporte;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.repository.SlotUnidadTransporteRepository;

@ExtendWith(MockitoExtension.class)
class UbicacionUnidadProveedorTest {

    @Mock private SlotUnidadTransporteRepository slotRepository;
    @InjectMocks private UbicacionUnidadProveedor proveedor;

    @Test
    void resuelveUnidades() {
        assertThat(proveedor.tipo()).isEqualTo(TipoUbicacion.UNIDAD);
    }

    @Test
    void devuelveLaPatenteYElCodigoDeLaPosicion() {
        SlotUnidadTransporte slot = unidadConNeumaticoMontado().getSlots().get(0);
        when(slotRepository.findConUnidadById(SLOT_ID)).thenReturn(Optional.of(slot));

        assertThat(proveedor.buscar(SLOT_ID)).contains(UbicacionOperativaDTO.deUnidad(PATENTE, "E1-IZQ"));
    }

    @Test
    void sinSlotDevuelveVacio() {
        when(slotRepository.findConUnidadById(SLOT_ID)).thenReturn(Optional.empty());

        assertThat(proveedor.buscar(SLOT_ID)).isEmpty();
    }

}

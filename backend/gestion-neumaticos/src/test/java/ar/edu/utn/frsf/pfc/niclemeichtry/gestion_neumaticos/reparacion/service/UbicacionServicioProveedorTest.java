package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.SERVICIO_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.servicioReparacion;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.servicioReparacionRequest;
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
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.entity.ServicioReparacion;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.repository.ServicioReparacionRepository;

@ExtendWith(MockitoExtension.class)
class UbicacionServicioProveedorTest {

    @Mock private ServicioReparacionRepository servicioRepository;
    @InjectMocks private UbicacionServicioProveedor proveedor;

    @Test
    void resuelveServicios() {
        assertThat(proveedor.tipo()).isEqualTo(TipoUbicacion.SERVICIO_REPARACION);
    }

    @Test
    void devuelveElNombreDelServicio() {
        ServicioReparacion servicio = servicioReparacion(servicioReparacionRequest());
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicio));

        assertThat(proveedor.buscar(SERVICIO_ID)).contains(UbicacionOperativaDTO.deServicio(servicio.getNombre()));
    }

    @Test
    void sinServicioDevuelveVacio() {
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.empty());

        assertThat(proveedor.buscar(SERVICIO_ID)).isEmpty();
    }

}

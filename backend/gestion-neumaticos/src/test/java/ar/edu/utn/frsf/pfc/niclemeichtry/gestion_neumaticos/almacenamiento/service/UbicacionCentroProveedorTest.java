package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.CENTRO_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.centroAlmacenamiento;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.centroAlmacenamientoRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.entity.CentroAlmacenamiento;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.repository.CentroAlmacenamientoRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.TipoUbicacion;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.UbicacionOperativaDTO;

@ExtendWith(MockitoExtension.class)
class UbicacionCentroProveedorTest {

    @Mock private CentroAlmacenamientoRepository centroRepository;
    @InjectMocks private UbicacionCentroProveedor proveedor;

    @Test
    void resuelveCentros() {
        assertThat(proveedor.tipo()).isEqualTo(TipoUbicacion.CENTRO_ALMACENAMIENTO);
    }

    @Test
    void devuelveElNombreDelCentro() {
        CentroAlmacenamiento centro = centroAlmacenamiento(centroAlmacenamientoRequest());
        when(centroRepository.findById(CENTRO_ID)).thenReturn(Optional.of(centro));

        assertThat(proveedor.buscar(CENTRO_ID)).contains(UbicacionOperativaDTO.deCentro(centro.getNombre()));
    }

    @Test
    void sinCentroDevuelveVacio() {
        when(centroRepository.findById(CENTRO_ID)).thenReturn(Optional.empty());

        assertThat(proveedor.buscar(CENTRO_ID)).isEmpty();
    }

}

package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.CENTRO_ALMACENAMIENTO_PUBLIC_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.centroAlmacenamiento;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.centroAlmacenamientoRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.entity.CentroAlmacenamiento;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.repository.CentroAlmacenamientoRepository;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.OperacionNoPermitidaException;

@ExtendWith(MockitoExtension.class)
class StockCentroAlmacenamientoImplTest {

    @Mock private CentroAlmacenamientoRepository centroRepository;
    @InjectMocks private StockCentroAlmacenamientoImpl stock;

    @Test
    @DisplayName("resuelve el id interno por publicId, o vacío si no existe")
    void idPorPublicId() {
        when(centroRepository.findByPublicId(CENTRO_ALMACENAMIENTO_PUBLIC_ID))
                .thenReturn(Optional.of(centroAlmacenamiento(centroAlmacenamientoRequest())));

        assertThat(stock.buscarIdPorPublicId(CENTRO_ALMACENAMIENTO_PUBLIC_ID)).contains(1L);
    }

    @Test
    @DisplayName("nombres por lote en una consulta; un lote vacío no consulta")
    void nombresPorLote() {
        when(centroRepository.findAllByIdIn(List.of(1L)))
                .thenReturn(List.of(centroAlmacenamiento(centroAlmacenamientoRequest())));

        assertThat(stock.buscarNombresPorIds(List.of(1L))).containsExactlyEntriesOf(java.util.Map.of(1L,
                "Depósito Central"));
        assertThat(new StockCentroAlmacenamientoImpl(centroRepository).buscarNombresPorIds(List.of())).isEmpty();
    }

    @Test
    @DisplayName("CA6: retirar un neumático descuenta uno del stock del centro bloqueado")
    void retira() {
        CentroAlmacenamiento centro = centroAlmacenamiento(centroAlmacenamientoRequest());
        centro.agregarNeumaticoStock();
        centro.agregarNeumaticoStock();
        when(centroRepository.findParaActualizarById(1L)).thenReturn(Optional.of(centro));

        stock.retirarNeumatico(1L);

        assertThat(centro.getCantidadNeumaticosEnStock()).isEqualTo(1);
    }

    @Test
    @DisplayName("un centro inexistente responde 404 y uno sin stock no queda negativo")
    void errores() {
        when(centroRepository.findParaActualizarById(9L)).thenReturn(Optional.empty());
        when(centroRepository.findParaActualizarById(1L))
                .thenReturn(Optional.of(centroAlmacenamiento(centroAlmacenamientoRequest())));

        assertThatThrownBy(() -> stock.retirarNeumatico(9L)).isInstanceOf(EntidadNoEncontradaException.class);
        assertThatThrownBy(() -> stock.retirarNeumatico(1L)).isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    @DisplayName("un lote vacío no consulta la base")
    void loteVacioSinConsulta() {
        assertThat(stock.buscarNombresPorIds(List.of())).isEmpty();

        verifyNoInteractions(centroRepository);
    }

}

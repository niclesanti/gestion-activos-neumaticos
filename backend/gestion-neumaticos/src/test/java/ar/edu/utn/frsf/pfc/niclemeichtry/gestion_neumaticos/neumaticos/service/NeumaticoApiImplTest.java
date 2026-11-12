package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.NEUMATICO_MONTADO_PUBLIC_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.NUMERO_SERIE;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.marca;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoMontado;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.NeumaticoMontadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.EstadoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.Neumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.repository.NeumaticoRepository;

@ExtendWith(MockitoExtension.class)
class NeumaticoApiImplTest {

    @Mock private NeumaticoRepository neumaticoRepository;
    @InjectMocks private NeumaticoApiImpl api;

    @Test
    @DisplayName("devuelve los datos de cada neumático por su id interno, con una sola consulta")
    void buscaPorLote() {
        Neumatico neumatico = Neumatico.builder().id(10L).publicId(NEUMATICO_MONTADO_PUBLIC_ID)
                .numeroSerie(NUMERO_SERIE).estadoActual(EstadoNeumatico.NUEVO).kilometrajeTotal(1500f)
                .marca(marca()).build();
        when(neumaticoRepository.findAllByIdIn(List.of(10L))).thenReturn(List.of(neumatico));

        Map<Long, NeumaticoMontadoDTO> resultado = api.buscarMontadosPorIds(List.of(10L));

        assertThat(resultado).containsOnlyKeys(10L);
        assertThat(resultado.get(10L)).isEqualTo(neumaticoMontado());
        verify(neumaticoRepository).findAllByIdIn(List.of(10L));
    }

    @Test
    @DisplayName("conserva como nulos el número de serie y el kilometraje no registrados")
    void datosNoRegistrados() {
        Neumatico neumatico = Neumatico.builder().id(11L).publicId(NEUMATICO_MONTADO_PUBLIC_ID)
                .estadoActual(EstadoNeumatico.RECAPADO).marca(marca()).build();
        when(neumaticoRepository.findAllByIdIn(List.of(11L))).thenReturn(List.of(neumatico));

        NeumaticoMontadoDTO dto = api.buscarMontadosPorIds(List.of(11L)).get(11L);

        assertThat(dto.numeroSerie()).isNull();
        assertThat(dto.kilometrajeTotal()).isNull();
        assertThat(dto.estado()).isEqualTo("RECAPADO");
    }

    @Test
    @DisplayName("un lote vacío no consulta la base")
    void loteVacio() {
        assertThat(api.buscarMontadosPorIds(List.of())).isEmpty();

        verifyNoInteractions(neumaticoRepository);
    }

}

package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.CENTRO_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.NEUMATICO_MONTADO_PUBLIC_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.NUMERO_SERIE;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.marca;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoEn;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.neumaticoMontado;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.EntidadNoEncontradaException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.NeumaticoNoDisponibleException;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.NeumaticoMontadoDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.NeumaticoMontajeDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api.StockCentroAlmacenamiento;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.EstadoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.Neumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.SituacionOperativa;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.TipoEjeUnidadTransporte;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.repository.NeumaticoRepository;

@ExtendWith(MockitoExtension.class)
class NeumaticoApiImplTest {

    @Mock private NeumaticoRepository neumaticoRepository;
    @Mock private StockCentroAlmacenamiento stockCentroAlmacenamiento;
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

    @Nested
    @DisplayName("montar")
    class Montar {

        private static final Long SLOT_DESTINO = 500L;

        private void guardaLoQueRecibe() {
            when(neumaticoRepository.saveAndFlush(any(Neumatico.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("CA2: uno en espera queda montado en el slot, fuera de espera, sin tocar el stock")
        void desdeEspera() {
            Neumatico neumatico = neumaticoEn(SituacionOperativa.EN_ESPERA);
            neumatico.setCentroAlmacenamientoId(null);
            when(neumaticoRepository.findByPublicId(neumatico.getPublicId())).thenReturn(Optional.of(neumatico));
            guardaLoQueRecibe();

            NeumaticoMontajeDTO montado = api.montar(neumatico.getPublicId(), SLOT_DESTINO);

            assertThat(montado).isEqualTo(new NeumaticoMontajeDTO(neumatico.getId(), neumatico.getPublicId(),
                    neumatico.getNumeroSerie()));
            assertThat(neumatico.getSituacionOperativaVigente()).isEqualTo(SituacionOperativa.MONTADO);
            assertThat(neumatico.getEnEspera()).isFalse();
            assertThat(neumatico.getSlotMontadoId()).isEqualTo(SLOT_DESTINO);
            verifyNoInteractions(stockCentroAlmacenamiento);
        }

        @Test
        @DisplayName("CA6: uno en stock sale del centro, que lo descuenta; tipo de eje y kilometraje no cambian")
        void desdeStock() {
            Neumatico neumatico = neumaticoEn(SituacionOperativa.EN_STOCK);
            TipoEjeUnidadTransporte tipoActual = neumatico.getTipoActual();
            Float kilometraje = neumatico.getKilometrajeTotal();
            when(neumaticoRepository.findByPublicId(neumatico.getPublicId())).thenReturn(Optional.of(neumatico));
            guardaLoQueRecibe();

            api.montar(neumatico.getPublicId(), SLOT_DESTINO);

            verify(stockCentroAlmacenamiento).retirarNeumatico(CENTRO_ID);
            assertThat(neumatico.getCentroAlmacenamientoId()).isNull();
            assertThat(neumatico.getSituacionOperativaVigente()).isEqualTo(SituacionOperativa.MONTADO);
            assertThat(neumatico.getTipoActual()).isEqualTo(tipoActual);
            assertThat(neumatico.getKilometrajeTotal()).isEqualTo(kilometraje);
        }

        @ParameterizedTest
        @EnumSource(value = SituacionOperativa.class, names = { "MONTADO", "EN_REPARACION", "DE_BAJA" })
        @DisplayName("CA8: montado, en reparación o de baja (situación) se rechaza con 409 sin cambios")
        void situacionNoDisponible(SituacionOperativa situacion) {
            Neumatico neumatico = neumaticoEn(situacion);
            when(neumaticoRepository.findByPublicId(neumatico.getPublicId())).thenReturn(Optional.of(neumatico));

            assertThatThrownBy(() -> api.montar(neumatico.getPublicId(), SLOT_DESTINO))
                    .isInstanceOf(NeumaticoNoDisponibleException.class);

            assertThat(neumatico.getSituacionOperativaVigente()).isEqualTo(situacion);
            verify(neumaticoRepository, never()).saveAndFlush(any());
            verifyNoInteractions(stockCentroAlmacenamiento);
        }

        @Test
        @DisplayName("CA8: de baja por estado o con baja lógica se rechaza con 409")
        void deBaja() {
            Neumatico estadoDeBaja = neumaticoEn(SituacionOperativa.EN_STOCK);
            estadoDeBaja.setEstadoActual(EstadoNeumatico.DE_BAJA);
            Neumatico conBajaLogica = neumaticoEn(SituacionOperativa.EN_ESPERA);
            conBajaLogica.setPublicId(UUID.randomUUID());
            conBajaLogica.darDeBaja(UUID.randomUUID(), Instant.now());
            when(neumaticoRepository.findByPublicId(estadoDeBaja.getPublicId())).thenReturn(Optional.of(estadoDeBaja));
            when(neumaticoRepository.findByPublicId(conBajaLogica.getPublicId())).thenReturn(Optional.of(conBajaLogica));

            assertThatThrownBy(() -> api.montar(estadoDeBaja.getPublicId(), SLOT_DESTINO))
                    .isInstanceOf(NeumaticoNoDisponibleException.class);
            assertThatThrownBy(() -> api.montar(conBajaLogica.getPublicId(), SLOT_DESTINO))
                    .isInstanceOf(NeumaticoNoDisponibleException.class)
                    .hasMessage("El neumático no está disponible para montar: tiene que estar en stock o en espera");
            verify(neumaticoRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("CA8: uno inexistente responde 404")
        void inexistente() {
            UUID publicId = UUID.randomUUID();
            when(neumaticoRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> api.montar(publicId, SLOT_DESTINO))
                    .isInstanceOf(EntidadNoEncontradaException.class)
                    .hasMessage(NeumaticoServiceImpl.MENSAJE_NEUMATICO_INEXISTENTE);
        }

    }

    @Nested
    @DisplayName("desmontar")
    class Desmontar {

        @Test
        @DisplayName("CA7: el montado queda en espera y sin slot, escrito en el momento")
        void quedaEnEspera() {
            Neumatico neumatico = neumaticoEn(SituacionOperativa.MONTADO);
            when(neumaticoRepository.findById(neumatico.getId())).thenReturn(Optional.of(neumatico));
            when(neumaticoRepository.saveAndFlush(neumatico)).thenReturn(neumatico);

            NeumaticoMontajeDTO desmontado = api.desmontar(neumatico.getId());

            assertThat(desmontado.publicId()).isEqualTo(neumatico.getPublicId());
            assertThat(neumatico.getSituacionOperativaVigente()).isEqualTo(SituacionOperativa.EN_ESPERA);
            assertThat(neumatico.getEnEspera()).isTrue();
            assertThat(neumatico.getSlotMontadoId()).isNull();
            verify(neumaticoRepository).saveAndFlush(neumatico);
        }

        @Test
        @DisplayName("uno inexistente responde 404")
        void inexistente() {
            when(neumaticoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> api.desmontar(99L)).isInstanceOf(EntidadNoEncontradaException.class);
        }

    }

}

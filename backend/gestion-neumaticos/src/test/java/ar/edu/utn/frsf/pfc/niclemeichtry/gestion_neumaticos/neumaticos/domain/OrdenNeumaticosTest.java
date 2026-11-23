package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class OrdenNeumaticosTest {

    @Test
    void esValido_sinOrden_true() {
        assertThat(OrdenNeumaticos.esValido(PageRequest.of(0, 10))).isTrue();
    }

    @Test
    void esValido_porNumeroDeSerie_true() {
        assertThat(OrdenNeumaticos.esValido(PageRequest.of(0, 10, Sort.by("numeroSerie")))).isTrue();
        assertThat(OrdenNeumaticos.esValido(PageRequest.of(0, 10, Sort.by("numeroSerie").descending()))).isTrue();
    }

    @Test
    void esValido_porOtroCampo_false() {
        assertThat(OrdenNeumaticos.esValido(PageRequest.of(0, 10, Sort.by("id")))).isFalse();
    }

    @Test
    void esValido_numeroDeSerieYOtroCampo_false() {
        assertThat(OrdenNeumaticos.esValido(PageRequest.of(0, 10, Sort.by("numeroSerie", "publicId")))).isFalse();
    }
}

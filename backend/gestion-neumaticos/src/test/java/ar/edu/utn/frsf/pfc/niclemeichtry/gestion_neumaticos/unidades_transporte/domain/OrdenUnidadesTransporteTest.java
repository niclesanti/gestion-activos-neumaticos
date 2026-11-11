package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.unidades_transporte.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class OrdenUnidadesTransporteTest {

    @Test
    void esValido_sinOrden_true() {
        assertThat(OrdenUnidadesTransporte.esValido(PageRequest.of(0, 10))).isTrue();
    }

    @Test
    void esValido_porPatente_true() {
        assertThat(OrdenUnidadesTransporte.esValido(PageRequest.of(0, 10, Sort.by("patente")))).isTrue();
        assertThat(OrdenUnidadesTransporte.esValido(PageRequest.of(0, 10, Sort.by("patente").descending()))).isTrue();
    }

    @Test
    void esValido_porOtroCampo_false() {
        assertThat(OrdenUnidadesTransporte.esValido(PageRequest.of(0, 10, Sort.by("id")))).isFalse();
    }

    @Test
    void esValido_patenteYOtroCampo_false() {
        assertThat(OrdenUnidadesTransporte.esValido(PageRequest.of(0, 10, Sort.by("patente", "publicId")))).isFalse();
    }
}

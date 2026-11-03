package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.OperacionNoPermitidaException;

class CentroAlmacenamientoTest {

    @Test
    @DisplayName("un centro nuevo no tiene neumáticos en stock")
    void empiezaEnCero() {
        assertThat(CentroAlmacenamiento.builder().nombre("Depósito Norte").build().getCantidadNeumaticosEnStock())
                .isZero();
        assertThat(new CentroAlmacenamiento().getCantidadNeumaticosEnStock()).isZero();
    }

    @Test
    @DisplayName("agregar un neumático al stock incrementa la cantidad")
    void agregarIncrementa() {
        CentroAlmacenamiento centro = CentroAlmacenamiento.builder().build();

        centro.agregarNeumaticoStock();
        centro.agregarNeumaticoStock();

        assertThat(centro.getCantidadNeumaticosEnStock()).isEqualTo(2);
    }

    @Test
    @DisplayName("remover un neumático del stock decrementa la cantidad")
    void removerDecrementa() {
        CentroAlmacenamiento centro = CentroAlmacenamiento.builder().cantidadNeumaticosEnStock(2).build();

        centro.removerNeumaticoStock();

        assertThat(centro.getCantidadNeumaticosEnStock()).isOne();
    }

    @Test
    @DisplayName("sin stock no se puede remover: la cantidad nunca queda negativa")
    void removerSinStock() {
        CentroAlmacenamiento centro = CentroAlmacenamiento.builder().build();

        assertThatThrownBy(centro::removerNeumaticoStock).isInstanceOf(OperacionNoPermitidaException.class);

        assertThat(centro.getCantidadNeumaticosEnStock()).isZero();
    }

}

package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.exception.OperacionNoPermitidaException;

class ServicioReparacionTest {

    @Test
    @DisplayName("un servicio nuevo no tiene neumáticos en reparación")
    void empiezaEnCero() {
        assertThat(ServicioReparacion.builder().nombre("Gomería Sur").build().getCantidadNeumaticosEnReparacion())
                .isZero();
        assertThat(new ServicioReparacion().getCantidadNeumaticosEnReparacion()).isZero();
    }

    @Test
    @DisplayName("enviar un neumático a reparación incrementa la cantidad")
    void enviarIncrementa() {
        ServicioReparacion servicio = ServicioReparacion.builder().build();

        servicio.enviarNeumaticoReparacion();
        servicio.enviarNeumaticoReparacion();

        assertThat(servicio.getCantidadNeumaticosEnReparacion()).isEqualTo(2);
    }

    @Test
    @DisplayName("reingresar un neumático decrementa la cantidad")
    void reingresarDecrementa() {
        ServicioReparacion servicio = ServicioReparacion.builder().cantidadNeumaticosEnReparacion(2).build();

        servicio.reingresarNeumatico();

        assertThat(servicio.getCantidadNeumaticosEnReparacion()).isOne();
    }

    @Test
    @DisplayName("sin neumáticos en reparación no se puede reingresar: la cantidad nunca queda negativa")
    void reingresarSinNeumaticos() {
        ServicioReparacion servicio = ServicioReparacion.builder().build();

        assertThatThrownBy(servicio::reingresarNeumatico).isInstanceOf(OperacionNoPermitidaException.class);

        assertThat(servicio.getCantidadNeumaticosEnReparacion()).isZero();
    }

}

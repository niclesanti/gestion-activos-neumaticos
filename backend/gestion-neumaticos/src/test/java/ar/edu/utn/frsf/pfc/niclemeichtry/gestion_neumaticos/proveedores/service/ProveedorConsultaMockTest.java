package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PROVEEDOR_PUBLIC_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.proveedorResumen;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.ProveedorResumen;

class ProveedorConsultaMockTest {

    private final ProveedorConsultaMock consulta = new ProveedorConsultaMock();

    @Test
    void buscaPorPublicId() {
        assertThat(consulta.buscarPorPublicId(PROVEEDOR_PUBLIC_ID)).contains(proveedorResumen());
    }

    @Test
    void unPublicIdDesconocidoNoDevuelveNada() {
        assertThat(consulta.buscarPorPublicId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void listaOrdenadoPorNombre() {
        assertThat(consulta.listar()).hasSize(3)
                .extracting(ProveedorResumen::nombre)
                .isSorted();
    }

}

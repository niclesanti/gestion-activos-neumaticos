package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PROVEEDOR_PUBLIC_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.proveedor;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.proveedorRequest;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.proveedorResumen;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.mapper.ProveedorMapperImpl;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.repository.ProveedorRepository;

@ExtendWith(MockitoExtension.class)
class ProveedorApiImplTest {

    @Mock private ProveedorRepository proveedorRepository;

    private ProveedorApiImpl proveedorApi;

    @BeforeEach
    void crearProveedorApi() {
        proveedorApi = new ProveedorApiImpl(proveedorRepository, new ProveedorMapperImpl());
    }

    @Test
    @DisplayName("busca por publicId y devuelve el resumen con el id interno")
    void buscaExistente() {
        when(proveedorRepository.findByPublicId(PROVEEDOR_PUBLIC_ID))
                .thenReturn(Optional.of(proveedor(proveedorRequest())));

        assertThat(proveedorApi.buscarPorPublicId(PROVEEDOR_PUBLIC_ID)).contains(proveedorResumen());
    }

    @Test
    @DisplayName("vacío si el proveedor no existe")
    void buscaInexistente() {
        UUID otro = UUID.randomUUID();
        when(proveedorRepository.findByPublicId(otro)).thenReturn(Optional.empty());

        assertThat(proveedorApi.buscarPorPublicId(otro)).isEmpty();
    }

    @Test
    @DisplayName("lista los resúmenes en el orden del repositorio")
    void lista() {
        when(proveedorRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(proveedor(proveedorRequest())));

        assertThat(proveedorApi.listar()).containsExactly(proveedorResumen());
    }

}

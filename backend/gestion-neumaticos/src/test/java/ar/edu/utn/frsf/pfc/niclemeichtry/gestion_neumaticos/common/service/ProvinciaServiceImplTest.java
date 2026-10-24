package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.PROVINCIA_ID;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.provincia;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.dto.ProvinciaResponseDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.mapper.DomicilioMapperImpl;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.repository.ProvinciaRepository;

@ExtendWith(MockitoExtension.class)
class ProvinciaServiceImplTest {

    @Mock private ProvinciaRepository provinciaRepository;

    private ProvinciaServiceImpl servicio;

    @BeforeEach
    void crearServicio() {
        servicio = new ProvinciaServiceImpl(provinciaRepository, new DomicilioMapperImpl());
    }

    @Test
    @DisplayName("listarProvincias devuelve id y nombre en el orden del repositorio")
    void listarProvincias() {
        when(provinciaRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(provincia()));

        assertThat(servicio.listarProvincias()).containsExactly(new ProvinciaResponseDTO(PROVINCIA_ID, "Santa Fe"));
    }

}

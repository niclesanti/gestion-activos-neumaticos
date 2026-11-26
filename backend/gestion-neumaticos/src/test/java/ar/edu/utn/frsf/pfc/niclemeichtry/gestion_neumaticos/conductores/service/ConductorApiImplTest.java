package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.service;

import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductor;
import static ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.TestDataFactory.conductorRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.api.ConductorResumenDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.domain.entity.Conductor;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.mapper.ConductorMapperImpl;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.conductores.repository.ConductorRepository;

@ExtendWith(MockitoExtension.class)
class ConductorApiImplTest {

    @Mock private ConductorRepository conductorRepository;

    private ConductorApiImpl api;

    @BeforeEach
    void crearApi() {
        api = new ConductorApiImpl(conductorRepository, new ConductorMapperImpl());
    }

    @Test
    @DisplayName("devuelve el id interno y el nombre del conductor activo")
    void activo() {
        Conductor conductor = conductor(conductorRequest());
        when(conductorRepository.findByPublicIdAndDeletedAtIsNull(conductor.getPublicId()))
                .thenReturn(Optional.of(conductor));

        assertThat(api.buscarActivoPorPublicId(conductor.getPublicId()))
                .contains(new ConductorResumenDTO(100L, conductor.getPublicId(), "Juan", "Pérez"));
    }

    @Test
    @DisplayName("vacío si no existe o está dado de baja")
    void inexistente() {
        UUID publicId = UUID.randomUUID();
        when(conductorRepository.findByPublicIdAndDeletedAtIsNull(publicId)).thenReturn(Optional.empty());

        assertThat(api.buscarActivoPorPublicId(publicId)).isEmpty();
    }

}

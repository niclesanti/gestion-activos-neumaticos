package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.service;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.almacenamiento.domain.dto.CentroAlmacenamientoResponseDTO;

public interface CentroAlmacenamientoService {

    /** Da de alta un centro de almacenamiento sin stock; {@code usuario} es el publicId de quien lo registra. */
    CentroAlmacenamientoResponseDTO registrarCentroAlmacenamiento(CentroAlmacenamientoRequestDTO request,
            String usuario);

}

package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.service;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.dto.ServicioReparacionRequestDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.reparacion.domain.dto.ServicioReparacionResponseDTO;

public interface ServicioReparacionService {

    /** Da de alta un servicio de reparación (HU-18); {@code usuario} es el publicId de quien lo registra. */
    ServicioReparacionResponseDTO registrarServicioReparacion(ServicioReparacionRequestDTO request, String usuario);

}

package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.mapper;

import org.mapstruct.Mapper;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.config.MapstructConfig;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.dto.UsuarioSesionDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.Usuario;

@Mapper(config = MapstructConfig.class)
public interface UsuarioMapper {

    UsuarioSesionDTO toSesionDTO(Usuario usuario);

}

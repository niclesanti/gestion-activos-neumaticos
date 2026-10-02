package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Login por nombre de usuario o email (ambos persistidos en minúsculas). */
    Optional<Usuario> findByNombreUsuarioOrEmail(String nombreUsuario, String email);

    Optional<Usuario> findByPublicId(UUID publicId);

}

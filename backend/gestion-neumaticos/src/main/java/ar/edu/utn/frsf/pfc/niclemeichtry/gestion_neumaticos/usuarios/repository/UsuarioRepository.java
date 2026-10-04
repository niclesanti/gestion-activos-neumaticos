package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.domain.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Login por nombre de usuario o email (ambos persistidos en minúsculas). Corre
     * sin usuario autenticado, así que no puede leer la tabla: usa la función
     * {@code SECURITY DEFINER} que solo expone esta búsqueda (ver V4__rls_usuarios.sql).
     */
    @Query(value = "SELECT * FROM usuarios.buscar_para_login(:identificador)", nativeQuery = true)
    Optional<Usuario> buscarParaLogin(String identificador);

    Optional<Usuario> findByPublicId(UUID publicId);

}

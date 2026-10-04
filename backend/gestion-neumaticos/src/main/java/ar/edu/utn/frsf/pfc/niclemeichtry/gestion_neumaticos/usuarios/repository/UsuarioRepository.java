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

    /**
     * Nivel de acceso actual del usuario, o vacío si ya no existe. Corre mientras
     * se autentica el token, todavía sin usuario en la base: usa la función
     * {@code SECURITY DEFINER} de V9__nivel_acceso_vigente.sql.
     */
    @Query(value = "SELECT usuarios.nivel_acceso_vigente(:publicId)", nativeQuery = true)
    Optional<String> nivelAccesoVigente(UUID publicId);

    Optional<Usuario> findByPublicId(UUID publicId);

}

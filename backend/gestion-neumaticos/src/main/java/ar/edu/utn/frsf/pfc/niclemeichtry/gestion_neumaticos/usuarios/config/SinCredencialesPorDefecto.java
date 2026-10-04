package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/**
 * En producción, la app no arranca si sigue habilitado algún usuario del seed de
 * desarrollo con su contraseña conocida (OWASP A07: credenciales por defecto).
 * Puede pasar si una base que corrió en {@code dev} pasa a {@code prod}: el seed
 * usa {@code ON CONFLICT DO NOTHING} y sus filas sobreviven.
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
public class SinCredencialesPorDefecto implements ApplicationRunner {

    /** Usuarios y contraseñas de {@code db/seed/dev/R__usuarios_dev.sql}. */
    static final Map<String, String> CREDENCIALES_SEED = Map.of(
            "administrador", "Admin.1234",
            "editor", "Editor.1234",
            "editor2", "Editor.1234",
            "lector", "Lector.1234",
            "lector2", "Lector.1234");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        List<String> expuestos = CREDENCIALES_SEED.entrySet().stream()
                .filter(seed -> usuarioRepository.buscarParaLogin(seed.getKey())
                        .filter(usuario -> passwordEncoder.matches(seed.getValue(), usuario.getClave()))
                        .isPresent())
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (!expuestos.isEmpty()) {
            throw new IllegalStateException("Hay usuarios de desarrollo con su contraseña por defecto en una base de "
                    + "producción: " + expuestos + ". Cambiarles la contraseña o eliminarlos antes de arrancar.");
        }
    }

}

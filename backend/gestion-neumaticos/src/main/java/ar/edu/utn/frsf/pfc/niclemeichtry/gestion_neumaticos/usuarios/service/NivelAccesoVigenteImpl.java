package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.security.NivelAccesoVigente;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/** El módulo de seguridad pide el rol vigente en cada request; la fuente es la tabla de usuarios. */
@Service
@RequiredArgsConstructor
public class NivelAccesoVigenteImpl implements NivelAccesoVigente {

    private final UsuarioRepository usuarioRepository;

    @Override
    public Optional<String> buscar(UUID publicId) {
        return usuarioRepository.nivelAccesoVigente(publicId);
    }

}

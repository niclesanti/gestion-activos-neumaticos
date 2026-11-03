package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.service;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.AuditoresResolver;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.auditoria.AuditorDTO;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.mapper.UsuarioMapper;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/**
 * Implementa el puerto de {@code common.auditoria}: resuelve en una sola consulta
 * los usuarios que firmaron las operaciones. Corre con el rol del administrador que
 * consulta, que por RLS puede leer todos los usuarios.
 */
@Service
@RequiredArgsConstructor
class AuditoresResolverImpl implements AuditoresResolver {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, AuditorDTO> resolver(Collection<UUID> publicIds) {
        if (publicIds.isEmpty()) {
            return Map.of();
        }
        return usuarioRepository.findAllByPublicIdIn(publicIds).stream()
                .map(usuarioMapper::toAuditorDTO)
                .collect(Collectors.toMap(AuditorDTO::publicId, Function.identity()));
    }
}

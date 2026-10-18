package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.ProveedorConsulta;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.proveedores.ProveedorResumen;

/**
 * Implementación provisoria (mock) de {@link ProveedorConsulta}: el módulo de
 * proveedores todavía no está desarrollado, así que devuelve un conjunto fijo
 * en memoria. Los UUID son constantes para que el frontend y los eventos ya
 * registrados sigan siendo válidos entre reinicios. Se reemplaza por la
 * implementación con persistencia sin tocar a sus consumidores.
 */
@Service
public class ProveedorConsultaMock implements ProveedorConsulta {

    static final List<ProveedorResumen> PROVEEDORES = List.of(
            new ProveedorResumen(1L, UUID.fromString("0199e8a0-0000-7000-8000-000000000001"),
                    "Neumáticos del Litoral S.A."),
            new ProveedorResumen(2L, UUID.fromString("0199e8a0-0000-7000-8000-000000000002"),
                    "Distribuidora Ruta 19 S.R.L."),
            new ProveedorResumen(3L, UUID.fromString("0199e8a0-0000-7000-8000-000000000003"),
                    "Gomería Industrial Santa Fe"));

    @Override
    public Optional<ProveedorResumen> buscarPorPublicId(UUID publicId) {
        return PROVEEDORES.stream().filter(proveedor -> proveedor.publicId().equals(publicId)).findFirst();
    }

    @Override
    public List<ProveedorResumen> listar() {
        return PROVEEDORES.stream().sorted(Comparator.comparing(ProveedorResumen::nombre)).toList();
    }

}

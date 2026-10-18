package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.common.domain.entity;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sin mapeo JPA todavía: se mapea como entidad (con su migración, RLS y
 * grants) junto con Provincia y Localidad cuando se implemente el módulo
 * common. Mientras tanto, mapearla rompería el arranque (ddl-auto=validate).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Domicilio {
    private Long id;
    private UUID publicId;
    private String calle;
    private String altura;
    private String piso;
    private String departamento;
    private Localidad localidad;
}

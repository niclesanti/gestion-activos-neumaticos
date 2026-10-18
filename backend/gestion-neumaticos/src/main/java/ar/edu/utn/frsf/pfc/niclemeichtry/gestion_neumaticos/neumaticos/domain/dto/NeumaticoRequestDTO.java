package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.dto;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.EstadoNeumatico;
import ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.domain.entity.TipoEjeUnidadTransporte;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Alta de un neumático. El frontend replica exactamente estas reglas en
 * {@code features/tires/schemas/tire-schema.ts}: si cambia una, cambian ambas.
 *
 * <p>La situación operativa no se recibe: todo neumático nuevo en el sistema
 * queda {@code EN_ESPERA} hasta que se opere con él.
 */
public record NeumaticoRequestDTO(
    // Opcional: algunas marcas genéricas no tienen número de serie.
    @Size(max = MAX_NUMERO_SERIE, message = "no puede superar los 100 caracteres")
    @Pattern(regexp = PATRON_NUMERO_SERIE,
            message = "solo admite letras, números, espacios, puntos, guiones y barras, y debe empezar con letra o número")
    String numeroSerie,

    @NotNull(message = "es obligatorio")
    EstadoNeumatico estadoActual,

    // Kilometrajes y recapados son opcionales (null = dato desconocido), salvo
    // donde el estado los exige (ver las reglas entre campos).
    @PositiveOrZero(message = "no puede ser negativo")
    @DecimalMax(value = MAX_KILOMETRAJE, message = "no puede superar los 9.999.999 km")
    Float kilometrajeTotal,

    @PositiveOrZero(message = "no puede ser negativo")
    @DecimalMax(value = MAX_KILOMETRAJE, message = "no puede superar los 9.999.999 km")
    Float kilometrajeUltimaReparacion,

    @NotNull(message = "es obligatorio")
    TipoEjeUnidadTransporte tipoOrigen,

    @NotNull(message = "es obligatorio")
    TipoEjeUnidadTransporte tipoActual,

    @Min(value = 0, message = "no puede ser negativa")
    @Max(value = MAX_RECAPADOS, message = "no puede superar los 20 recapados")
    Integer cantidadRecapados,

    @Size(max = MAX_COMENTARIO, message = "no puede superar los 512 caracteres")
    String comentario,

    @NotNull(message = "es obligatoria")
    @Positive(message = "es inválida")
    Long marcaId,

    // Solo se usa si el neumático es nuevo (evento de compra); es opcional.
    // Para cualquier otro estado se ignora.
    UUID proveedorId
) {

    public static final int MAX_NUMERO_SERIE = 100;
    // Vacío o en blanco = sin número de serie; los espacios de los extremos se recortan al guardar.
    public static final String PATRON_NUMERO_SERIE = "^\\s*([A-Za-z0-9][A-Za-z0-9 ./-]*)?\\s*$";
    public static final String MAX_KILOMETRAJE = "9999999";
    public static final int MAX_RECAPADOS = 20;
    public static final int MAX_COMENTARIO = 512;

    private static final Set<EstadoNeumatico> ESTADOS_RECAPADOS = EnumSet.of(
            EstadoNeumatico.RECAPADO, EstadoNeumatico.RECAPADO_USADO, EstadoNeumatico.RECAPADO_SIN_GARANTIA);

    /*
     * Reglas entre campos. Cada una es una propiedad booleana: Bean Validation
     * la informa como error del campo (ej. "estadoActualRegistrable: ...") y
     * se omite (true) si faltan datos que ya reporta su @NotNull. Los datos
     * opcionales que el estado vuelve obligatorios se exigen acá.
     */

    @JsonIgnore
    @AssertTrue(message = "no se puede registrar un neumático dado de baja")
    public boolean isEstadoActualRegistrable() {
        return estadoActual != EstadoNeumatico.DE_BAJA;
    }

    @JsonIgnore
    @AssertTrue(message = "el kilometraje desde la última reparación no puede superar el kilometraje total")
    public boolean isKilometrajeUltimaReparacionValido() {
        return kilometrajeTotal == null || kilometrajeUltimaReparacion == null
                || kilometrajeUltimaReparacion <= kilometrajeTotal;
    }

    @JsonIgnore
    @AssertTrue(message = "un neumático nuevo debe indicar kilometraje 0 y ningún recapado")
    public boolean isNuevoSinUso() {
        if (estadoActual != EstadoNeumatico.NUEVO) {
            return true;
        }
        // En un neumático nuevo el uso se conoce: los tres datos son obligatorios.
        return esCero(kilometrajeTotal) && esCero(kilometrajeUltimaReparacion)
                && cantidadRecapados != null && cantidadRecapados == 0;
    }

    @JsonIgnore
    @AssertTrue(message = "un neumático recapado debe indicar al menos un recapado")
    public boolean isRecapadoConRecapados() {
        return !ESTADOS_RECAPADOS.contains(estadoActual) || (cantidadRecapados != null && cantidadRecapados >= 1);
    }

    private static boolean esCero(Float valor) {
        return valor != null && valor == 0f;
    }

}

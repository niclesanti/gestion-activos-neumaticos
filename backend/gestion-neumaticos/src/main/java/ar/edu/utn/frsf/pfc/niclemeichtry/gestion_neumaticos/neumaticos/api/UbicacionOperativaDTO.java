package ar.edu.utn.frsf.pfc.niclemeichtry.gestion_neumaticos.neumaticos.api;

/**
 * Ubicación vigente de un neumático. Solo viene completo lo que corresponde a su situación: unidad y
 * posición (montado), centro de almacenamiento (en stock) o servicio de reparación (en reparación).
 */
public record UbicacionOperativaDTO(
    String unidad,
    String posicion,
    String centroAlmacenamiento,
    String servicioReparacion
) {

    public static UbicacionOperativaDTO vacia() {
        return new UbicacionOperativaDTO(null, null, null, null);
    }

    public static UbicacionOperativaDTO deUnidad(String patente, String posicion) {
        return new UbicacionOperativaDTO(patente, posicion, null, null);
    }

    public static UbicacionOperativaDTO deCentro(String nombre) {
        return new UbicacionOperativaDTO(null, null, nombre, null);
    }

    public static UbicacionOperativaDTO deServicio(String nombre) {
        return new UbicacionOperativaDTO(null, null, null, nombre);
    }

}

import { apiClient } from "@/lib/api/client"

/**
 * Proveedor como opción del select (`ProveedorListadoDTO` del backend). Vive en
 * este feature porque un feature no importa a otro (`features/suppliers` tiene
 * el alta); si otro feature también lo necesita, se muda a `src/lib`.
 */
export type Supplier = {
  publicId: string
  nombre: string
}

export async function getSuppliers() {
  const { data } = await apiClient.get<Supplier[]>("/api/proveedores")
  return data
}

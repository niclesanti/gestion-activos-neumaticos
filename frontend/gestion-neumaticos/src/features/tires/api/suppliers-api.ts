import { apiClient } from "@/lib/api/client"

/**
 * Proveedor (`ProveedorResponseDTO` del backend). Vive en este feature porque
 * todavía no existe uno de proveedores y un feature no importa a otro: cuando
 * se cree, este archivo se muda allá o a `src/lib`.
 */
export type Supplier = {
  publicId: string
  nombre: string
}

export async function getSuppliers() {
  const { data } = await apiClient.get<Supplier[]>("/api/proveedores")
  return data
}

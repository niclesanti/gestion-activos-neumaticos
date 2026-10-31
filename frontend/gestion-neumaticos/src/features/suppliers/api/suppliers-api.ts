import type { SupplierFormValues } from "@/features/suppliers/schemas/supplier-schema"
import type { Address } from "@/lib/address"
import { apiClient } from "@/lib/api/client"

/** Proveedor (`ProveedorResponseDTO` del backend). */
export type Supplier = {
  publicId: string
  nombre: string
  email: string | null
  telefono: string | null
  domicilio: Address | null
}

export async function registerSupplier(values: SupplierFormValues) {
  const { data } = await apiClient.post<Supplier>("/api/v1/proveedores", values)
  return data
}

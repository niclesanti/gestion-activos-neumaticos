import type { SupplierFormValues } from "@/features/suppliers/schemas/supplier-schema"
import { apiClient } from "@/lib/api/client"

/** Provincia (`ProvinciaResponseDTO` del backend). */
export type Province = {
  id: number
  nombre: string
}

/** Domicilio (`DomicilioResponseDTO` del backend). */
export type Address = {
  calle: string
  altura: string
  piso: string | null
  departamento: string | null
  localidad: string
  codigoPostal: string | null
  provinciaId: number
  provincia: string
}

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

export async function getProvinces() {
  const { data } = await apiClient.get<Province[]>("/api/v1/provincias")
  return data
}

import type { components } from '../../../../packages/openapi/generated/streamora-v1'
import { apiRequest } from './http'

export type ActivePet = components['schemas']['ActivePetView']
type ActivePetResponse = components['schemas']['ActivePetResponse']

export async function getActivePet(): Promise<ActivePet> {
  const response = await apiRequest<ActivePetResponse>('/api/v1/pets/active')
  return response.data
}

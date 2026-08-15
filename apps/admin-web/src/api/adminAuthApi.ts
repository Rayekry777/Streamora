import type { components } from '../../../../packages/openapi/generated/streamora-v1'
import { adminApiRequest } from './http'

export type AdminSession = components['schemas']['AdminSessionView']
type AdminSessionResponse = components['schemas']['AdminSessionResponse']
type LoginRequest = components['schemas']['LoginRequest']

export async function loginAdmin(request: LoginRequest): Promise<AdminSession> {
  const response = await adminApiRequest<AdminSessionResponse>('/admin-api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  return response.data
}

export async function getAdminSession(): Promise<AdminSession> {
  const response = await adminApiRequest<AdminSessionResponse>('/admin-api/v1/auth/session')
  return response.data
}

export async function logoutAdmin(csrfToken: string): Promise<void> {
  await adminApiRequest<void>('/admin-api/v1/auth/logout', {
    method: 'POST',
    headers: { 'X-CSRF-Token': csrfToken },
  })
}

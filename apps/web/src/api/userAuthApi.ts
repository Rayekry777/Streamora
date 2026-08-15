import type { components } from '../../../../packages/openapi/generated/streamora-v1'
import { apiRequest } from './http'

export type AuthSession = components['schemas']['AuthSessionView']
type AuthSessionResponse = components['schemas']['AuthSessionResponse']
type LoginRequest = components['schemas']['LoginRequest']
type RegistrationRequest = components['schemas']['UserRegistrationRequest']

export async function loginUser(request: LoginRequest): Promise<AuthSession> {
  const response = await apiRequest<AuthSessionResponse>('/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  return response.data
}

export async function registerUser(request: RegistrationRequest): Promise<AuthSession> {
  const response = await apiRequest<AuthSessionResponse>('/api/v1/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })
  return response.data
}

export async function getUserSession(): Promise<AuthSession> {
  const response = await apiRequest<AuthSessionResponse>('/api/v1/auth/session')
  return response.data
}

export async function logoutUser(csrfToken: string): Promise<void> {
  await apiRequest<void>('/api/v1/auth/logout', {
    method: 'POST',
    headers: { 'X-CSRF-Token': csrfToken },
  })
}

import type { components } from '../../../../packages/openapi/generated/streamora-v1'
import { adminApiRequest } from './http'

type OverviewResponse = components['schemas']['OperationsOverviewResponse']
export type OperationsOverview = components['schemas']['OperationsOverviewView']

export async function getOperationsOverview(): Promise<OperationsOverview> {
  const response = await adminApiRequest<OverviewResponse>('/admin-api/v1/operations/overview')
  return response.data
}

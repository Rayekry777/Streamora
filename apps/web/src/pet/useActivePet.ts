import { useQuery } from '@tanstack/vue-query'
import { computed } from 'vue'
import { getActivePet } from '../api/petApi'
import { useUserAuthStore } from '../stores/userAuth'

export function useActivePet() {
  const auth = useUserAuthStore()
  return useQuery({
    queryKey: computed(() => ['active-pet', auth.subjectKey]),
    queryFn: getActivePet,
    staleTime: 30_000,
  })
}

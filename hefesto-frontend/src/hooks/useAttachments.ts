import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  deleteAttachment,
  listAttachments,
  uploadAttachment,
} from '@/api/attachments'

export function useAttachments() {
  return useQuery({
    queryKey: ['attachments'],
    queryFn: listAttachments,
    staleTime: 5_000,
  })
}

export function useUploadAttachment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (file: File) => uploadAttachment(file),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['attachments'] })
    },
  })
}

export function useDeleteAttachment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deleteAttachment(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['attachments'] })
    },
  })
}

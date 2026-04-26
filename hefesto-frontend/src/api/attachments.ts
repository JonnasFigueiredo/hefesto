import type { Attachment } from '@/types/attachment'

/**
 * Upload de arquivo via multipart/form-data. NÃO usa o wrapper http() porque
 * precisa de FormData (sem Content-Type manual; o browser seta com boundary).
 */
export async function uploadAttachment(file: File): Promise<Attachment> {
  const fd = new FormData()
  fd.append('file', file)
  const res = await fetch('/api/attachments', {
    method: 'POST',
    body: fd,
  })
  if (!res.ok) {
    const body = await res.text().catch(() => '')
    throw new Error(`Upload falhou (${res.status}): ${body}`)
  }
  return (await res.json()) as Attachment
}

export async function listAttachments(): Promise<Attachment[]> {
  const res = await fetch('/api/attachments')
  if (!res.ok) throw new Error(`Falha ao listar anexos (${res.status})`)
  return (await res.json()) as Attachment[]
}

export async function deleteAttachment(id: string): Promise<void> {
  const res = await fetch(`/api/attachments/${id}`, { method: 'DELETE' })
  if (!res.ok && res.status !== 404) {
    throw new Error(`Falha ao remover anexo (${res.status})`)
  }
}

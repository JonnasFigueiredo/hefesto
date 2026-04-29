import { http } from './http'
import type { TestCase, TestCaseEvidence, TestCaseStatus } from '@/types/testCase'

export function listTestCasesByConversation(conversationId: string): Promise<TestCase[]> {
  return http<TestCase[]>(
    `/api/test-cases?conversationId=${encodeURIComponent(conversationId)}`,
  )
}

export function listTestCasesByMessage(messageId: string): Promise<TestCase[]> {
  return http<TestCase[]>(
    `/api/test-cases?messageId=${encodeURIComponent(messageId)}`,
  )
}

export function updateTestCaseStatus(
  id: string,
  status: TestCaseStatus,
  notes?: string | null,
): Promise<TestCase> {
  return http<TestCase>(`/api/test-cases/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    body: JSON.stringify({ status, notes: notes ?? null }),
  })
}

export async function deleteTestCase(id: string): Promise<void> {
  const res = await fetch(`/api/test-cases/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  })
  if (!res.ok && res.status !== 404) {
    throw new Error(`Falha ao deletar caso (${res.status})`)
  }
}

// ---- Evidence ----

export function listEvidence(testCaseId: string): Promise<TestCaseEvidence[]> {
  return http<TestCaseEvidence[]>(
    `/api/test-cases/${encodeURIComponent(testCaseId)}/evidence`,
  )
}

export async function uploadEvidence(
  testCaseId: string,
  file: File,
  note?: string,
): Promise<TestCaseEvidence> {
  const fd = new FormData()
  fd.append('file', file)
  if (note) fd.append('note', note)
  const res = await fetch(
    `/api/test-cases/${encodeURIComponent(testCaseId)}/evidence`,
    { method: 'POST', body: fd },
  )
  if (!res.ok) {
    const body = await res.text().catch(() => '')
    throw new Error(`Upload falhou (${res.status}): ${body}`)
  }
  return (await res.json()) as TestCaseEvidence
}

export async function deleteEvidence(testCaseId: string, evidenceId: string): Promise<void> {
  const res = await fetch(
    `/api/test-cases/${encodeURIComponent(testCaseId)}/evidence/${encodeURIComponent(evidenceId)}`,
    { method: 'DELETE' },
  )
  if (!res.ok && res.status !== 404) {
    throw new Error(`Falha ao deletar evidência (${res.status})`)
  }
}

/** URL pra <img src> ou link de download. Não passa pelo wrapper http. */
export function evidenceDownloadUrl(testCaseId: string, evidenceId: string): string {
  return `/api/test-cases/${encodeURIComponent(testCaseId)}/evidence/${encodeURIComponent(evidenceId)}/download`
}

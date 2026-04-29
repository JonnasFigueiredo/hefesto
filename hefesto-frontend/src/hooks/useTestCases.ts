import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  deleteEvidence,
  deleteTestCase,
  listEvidence,
  listTestCasesByConversation,
  listTestCasesByMessage,
  updateTestCaseStatus,
  uploadEvidence,
} from '@/api/testCases'
import type { TestCaseStatus } from '@/types/testCase'

export function useTestCasesByConversation(conversationId: string | null) {
  return useQuery({
    queryKey: ['test-cases', 'conv', conversationId],
    queryFn: () => listTestCasesByConversation(conversationId as string),
    enabled: !!conversationId,
    staleTime: 5_000,
  })
}

export function useTestCasesByMessage(messageId: string | null) {
  return useQuery({
    queryKey: ['test-cases', 'msg', messageId],
    queryFn: () => listTestCasesByMessage(messageId as string),
    enabled: !!messageId,
    staleTime: 5_000,
  })
}

export function useUpdateTestCaseStatus() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({
      id,
      status,
      notes,
    }: {
      id: string
      status: TestCaseStatus
      notes?: string | null
    }) => updateTestCaseStatus(id, status, notes),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['test-cases'] })
    },
  })
}

export function useDeleteTestCase() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => deleteTestCase(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['test-cases'] })
    },
  })
}

export function useEvidence(testCaseId: string | null) {
  return useQuery({
    queryKey: ['test-case-evidence', testCaseId],
    queryFn: () => listEvidence(testCaseId as string),
    enabled: !!testCaseId,
    staleTime: 5_000,
  })
}

export function useUploadEvidence() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({
      testCaseId,
      file,
      note,
    }: {
      testCaseId: string
      file: File
      note?: string
    }) => uploadEvidence(testCaseId, file, note),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({
        queryKey: ['test-case-evidence', variables.testCaseId],
      })
    },
  })
}

export function useDeleteEvidence() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({
      testCaseId,
      evidenceId,
    }: {
      testCaseId: string
      evidenceId: string
    }) => deleteEvidence(testCaseId, evidenceId),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({
        queryKey: ['test-case-evidence', variables.testCaseId],
      })
    },
  })
}

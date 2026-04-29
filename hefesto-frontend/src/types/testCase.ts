export type TestCaseStatus = 'PENDING' | 'PASSED' | 'FAILED' | 'BLOCKED'

export type TestCaseCategory = 'POSITIVO' | 'NEGATIVO' | 'EDGE' | string

export interface TestCase {
  id: string
  conversationId: string
  messageId: string | null
  code: string | null
  category: TestCaseCategory | null
  title: string
  preconditions: string | null
  steps: string[]
  expectedResult: string | null
  priority: string | null
  status: TestCaseStatus
  notes: string | null
  position: number
  createdAt: number
  updatedAt: number
}

export interface TestCaseEvidence {
  id: string
  testCaseId: string
  filename: string
  contentType: string | null
  sizeBytes: number
  note: string | null
  uploadedAt: number
}

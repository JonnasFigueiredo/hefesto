import { http } from './http'
import type {
  CreateIssuePayload,
  CreatedIssue,
  JiraComment,
  JiraIssue,
  JiraIssuePage,
  JiraProjectRef,
  JiraStatusResponse,
  JiraUser,
  CoverageReport,
  StoryDraft,
  StoryDraftPayload,
  StoryReview,
  TestSubtasksResult,
} from '@/types/jira'

export function getJiraStatus(): Promise<JiraStatusResponse> {
  return http<JiraStatusResponse>('/api/jira/status')
}

export function getMyself(): Promise<JiraUser> {
  return http<JiraUser>('/api/jira/myself')
}

export interface SearchParams {
  jql: string
  nextPageToken?: string | null
  maxResults?: number
}

export function searchIssues({
  jql,
  nextPageToken,
  maxResults = 20,
}: SearchParams): Promise<JiraIssuePage> {
  const qs = new URLSearchParams({
    jql,
    maxResults: String(maxResults),
  })
  if (nextPageToken) qs.set('nextPageToken', nextPageToken)
  return http<JiraIssuePage>(`/api/jira/issues?${qs.toString()}`)
}

export function getIssue(key: string): Promise<JiraIssue> {
  return http<JiraIssue>(`/api/jira/issues/${encodeURIComponent(key)}`)
}

export function getIssueComments(key: string): Promise<JiraComment[]> {
  return http<JiraComment[]>(`/api/jira/issues/${encodeURIComponent(key)}/comments`)
}

export function createIssue(payload: CreateIssuePayload): Promise<CreatedIssue> {
  return http<CreatedIssue>('/api/jira/issues', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function getProjects(): Promise<JiraProjectRef[]> {
  return http<JiraProjectRef[]>('/api/jira/projects')
}

export function getProjectIssueTypes(key: string): Promise<string[]> {
  return http<string[]>(`/api/jira/projects/${encodeURIComponent(key)}/issuetypes`)
}

export function draftStory(payload: StoryDraftPayload): Promise<StoryDraft> {
  return http<StoryDraft>('/api/jira/ai/draft-story', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function draftStoryFromImage(image: File, context?: string): Promise<StoryDraft> {
  // multipart: não usa o helper http() (que força Content-Type JSON);
  // o browser define o boundary do form-data sozinho.
  const fd = new FormData()
  fd.append('image', image)
  if (context) fd.append('context', context)
  const res = await fetch('/api/jira/ai/draft-story-from-image', { method: 'POST', body: fd })
  if (!res.ok) {
    const body = await res.text().catch(() => '')
    throw new Error(`${res.status} ${res.statusText} :: ${body}`)
  }
  return (await res.json()) as StoryDraft
}

export function reviewStory(payload: {
  model: string
  jiraKey: string
  postComment?: boolean
}): Promise<StoryReview> {
  return http<StoryReview>('/api/jira/ai/review-story', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function createTestSubtasks(payload: {
  model: string
  parentKey: string
  context?: string
}): Promise<TestSubtasksResult> {
  return http<TestSubtasksResult>('/api/jira/ai/test-subtasks', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function coverageReport(payload: {
  model: string
  jiraKey: string
}): Promise<CoverageReport> {
  return http<CoverageReport>('/api/jira/ai/coverage', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

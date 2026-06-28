import { http } from './http'
import type {
  CreateIssuePayload,
  CreatedIssue,
  JiraComment,
  JiraIssue,
  JiraIssuePage,
  JiraStatusResponse,
  JiraUser,
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

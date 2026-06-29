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

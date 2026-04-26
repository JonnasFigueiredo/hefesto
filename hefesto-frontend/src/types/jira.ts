export type JiraStatusCategory = 'new' | 'indeterminate' | 'done' | string

export interface JiraStatus {
  name: string
  category: JiraStatusCategory
}

export interface JiraUser {
  accountId: string | null
  displayName: string
  email: string | null
  avatarUrl: string | null
}

export interface JiraComment {
  id: string
  author: JiraUser | null
  body: string
  created: number
  updated: number
}

export interface JiraIssue {
  key: string
  summary: string
  status: JiraStatus | null
  issueType: string | null
  priority: string | null
  assignee: JiraUser | null
  reporter: JiraUser | null
  description: string | null
  acceptanceCriteria: string[]
  comments: JiraComment[]
  labels: string[]
  sprint: string | null
  created: number
  updated: number
  url: string | null
}

export interface JiraIssueListItem {
  key: string
  summary: string
  status: JiraStatus | null
  issueType: string | null
  priority: string | null
  assignee: JiraUser | null
  updated: number
  url: string | null
}

export interface JiraIssuePage {
  issues: JiraIssueListItem[]
  pageSize: number
  nextPageToken: string | null
  isLast: boolean
  maxResults: number
}

export interface JiraStatusResponse {
  configured: boolean
}

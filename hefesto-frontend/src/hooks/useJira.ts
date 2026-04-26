import { useQuery } from '@tanstack/react-query'
import { getIssue, getJiraStatus, searchIssues } from '@/api/jira'

export function useJiraStatus() {
  return useQuery({
    queryKey: ['jira', 'status'],
    queryFn: getJiraStatus,
    staleTime: 30_000,
  })
}

export function useJiraSearch(jql: string, enabled: boolean = true) {
  return useQuery({
    queryKey: ['jira', 'search', jql],
    queryFn: () => searchIssues({ jql }),
    enabled: enabled && jql.trim().length > 0,
    staleTime: 10_000,
  })
}

export function useJiraIssue(key: string | null) {
  return useQuery({
    queryKey: ['jira', 'issue', key],
    queryFn: () => getIssue(key as string),
    enabled: !!key,
    staleTime: 10_000,
  })
}

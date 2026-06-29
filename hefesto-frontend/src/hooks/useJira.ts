import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createIssue,
  draftStory,
  getIssue,
  getJiraStatus,
  getProjectIssueTypes,
  getProjects,
  searchIssues,
} from '@/api/jira'

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

export function useJiraProjects(enabled: boolean = true) {
  return useQuery({
    queryKey: ['jira', 'projects'],
    queryFn: getProjects,
    enabled,
    staleTime: 60_000,
  })
}

export function useJiraIssueTypes(projectKey: string | null) {
  return useQuery({
    queryKey: ['jira', 'issuetypes', projectKey],
    queryFn: () => getProjectIssueTypes(projectKey as string),
    enabled: !!projectKey,
    staleTime: 60_000,
  })
}

export function useCreateIssue() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: createIssue,
    onSuccess: () => {
      // Reexecuta a busca pra a história recém-criada poder aparecer na lista.
      qc.invalidateQueries({ queryKey: ['jira', 'search'] })
    },
  })
}

export function useDraftStory() {
  return useMutation({ mutationFn: draftStory })
}

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createIssue,
  createTestSubtasks,
  draftStory,
  draftStoryFromImage,
  getIssue,
  getJiraStatus,
  getProjectIssueTypes,
  getProjects,
  reviewStory,
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

export function useDraftStoryFromImage() {
  return useMutation({
    mutationFn: ({ image, context }: { image: File; context?: string }) =>
      draftStoryFromImage(image, context),
  })
}

export function useReviewStory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: reviewStory,
    onSuccess: (_data, vars) => {
      // Se comentou no Jira, recarrega a issue pra o comentário aparecer.
      if (vars.postComment) qc.invalidateQueries({ queryKey: ['jira', 'issue', vars.jiraKey] })
    },
  })
}

export function useCreateTestSubtasks() {
  return useMutation({ mutationFn: createTestSubtasks })
}

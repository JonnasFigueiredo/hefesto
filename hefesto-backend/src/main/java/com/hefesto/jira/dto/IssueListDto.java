package com.hefesto.jira.dto;

import java.util.List;

/** Item simplificado para a lista (sem descrição completa nem comentários). */
public record IssueListDto(
    String key,
    String summary,
    JiraStatusDto status,
    String issueType,
    String priority,
    JiraUserDto assignee,
    long updated,
    String url
) {

    /**
     * Página de resultados do search/jql.
     *
     * @param issues          itens desta página.
     * @param pageSize        quantos vieram nesta página.
     * @param nextPageToken   token pra próxima página; null no último.
     * @param isLast          true se esta é a última página.
     * @param maxResults      tamanho máximo solicitado.
     */
    public record Page(
        List<IssueListDto> issues,
        int pageSize,
        String nextPageToken,
        boolean isLast,
        int maxResults
    ) {}
}

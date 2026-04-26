package com.hefesto.chat;

import java.util.List;

import org.springframework.stereotype.Component;

import com.hefesto.agents.Agent;
import com.hefesto.attachments.Attachment;
import com.hefesto.jira.dto.IssueDto;

/**
 * Constrói o prompt final em camadas hierárquicas que o LLM recebe.
 *
 * <p>Estrutura do prompt resultante:</p>
 * <pre>
 * &lt;agent.systemPrompt&gt;
 *
 * # CONTEXTO ANEXADO
 *
 * ## Arquivo: manual.txt
 * &lt;content&gt;
 *
 * ## Issue Jira: PROJ-123 — Título
 * **Status**: ...
 * **Critérios**:
 *   - ...
 * **Descrição**:
 * &lt;markdown&gt;
 *
 * # MENSAGEM
 *
 * &lt;userMessage&gt;
 * </pre>
 */
@Component
public class PromptBuilder {

    public static final int MAX_TOTAL_CONTEXT_CHARS = 200_000;

    public String build(
        Agent agent,
        List<Attachment> attachments,
        IssueDto jiraIssue,
        String userMessage
    ) {
        StringBuilder out = new StringBuilder();
        boolean hasContext = (attachments != null && !attachments.isEmpty()) || jiraIssue != null;

        // Camada 1: persona / system prompt do agente.
        if (agent != null && agent.hasSystemPrompt()) {
            out.append(agent.systemPrompt().strip()).append("\n\n");
        }

        // Camada 2: contexto anexado (arquivos + Jira).
        if (hasContext) {
            out.append("# CONTEXTO ANEXADO\n\n");

            int budget = MAX_TOTAL_CONTEXT_CHARS;

            if (attachments != null) {
                for (Attachment a : attachments) {
                    if (budget <= 0) {
                        out.append("> _(demais arquivos truncados por limite de contexto)_\n\n");
                        break;
                    }
                    String section = renderAttachment(a, budget);
                    out.append(section);
                    budget -= section.length();
                }
            }

            if (jiraIssue != null) {
                out.append(renderIssue(jiraIssue));
            }
        }

        // Camada 3: mensagem do usuário.
        out.append("# MENSAGEM\n\n");
        out.append(userMessage == null ? "" : userMessage.strip());

        return out.toString();
    }

    private String renderAttachment(Attachment a, int charBudget) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Arquivo: ").append(a.filename()).append('\n');
        sb.append("_(").append(a.sizeBytes()).append(" bytes)_\n\n");

        String content = a.content() == null ? "" : a.content();
        boolean truncated = false;
        // Reserva ~200 chars pra cabeçalho/rodapé.
        int maxBody = Math.max(0, charBudget - 200);
        if (content.length() > maxBody) {
            content = content.substring(0, maxBody);
            truncated = true;
        }

        sb.append("```\n");
        sb.append(content);
        if (!content.endsWith("\n")) sb.append('\n');
        sb.append("```\n");
        if (truncated) {
            sb.append("\n_(arquivo truncado — apenas ").append(maxBody)
                .append(" caracteres iniciais incluídos)_\n");
        }
        sb.append('\n');
        return sb.toString();
    }

    private String renderIssue(IssueDto issue) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Issue Jira: ").append(issue.key());
        if (issue.summary() != null && !issue.summary().isBlank()) {
            sb.append(" — ").append(issue.summary());
        }
        sb.append('\n');

        if (issue.status() != null && issue.status().name() != null) {
            sb.append("**Status**: ").append(issue.status().name()).append("  \n");
        }
        if (issue.issueType() != null) {
            sb.append("**Tipo**: ").append(issue.issueType()).append("  \n");
        }
        if (issue.priority() != null) {
            sb.append("**Prioridade**: ").append(issue.priority()).append("  \n");
        }
        if (issue.assignee() != null && issue.assignee().displayName() != null) {
            sb.append("**Responsável**: ").append(issue.assignee().displayName()).append("  \n");
        }
        if (issue.labels() != null && !issue.labels().isEmpty()) {
            sb.append("**Labels**: ").append(String.join(", ", issue.labels())).append("  \n");
        }
        if (issue.sprint() != null) {
            sb.append("**Sprint**: ").append(issue.sprint()).append("  \n");
        }
        sb.append('\n');

        if (issue.acceptanceCriteria() != null && !issue.acceptanceCriteria().isEmpty()) {
            sb.append("**Critérios de aceite**:\n");
            for (String c : issue.acceptanceCriteria()) {
                sb.append("- ").append(c).append('\n');
            }
            sb.append('\n');
        }

        if (issue.description() != null && !issue.description().isBlank()) {
            sb.append("**Descrição**:\n\n");
            sb.append(issue.description().strip()).append("\n\n");
        }

        if (issue.comments() != null && !issue.comments().isEmpty()) {
            sb.append("**Comentários recentes** (últimos ")
                .append(Math.min(5, issue.comments().size())).append("):\n\n");
            int from = Math.max(0, issue.comments().size() - 5);
            for (int i = from; i < issue.comments().size(); i++) {
                var c = issue.comments().get(i);
                String author = c.author() == null ? "?" : c.author().displayName();
                sb.append("- _").append(author).append("_: ");
                String body = c.body() == null ? "" : c.body().replaceAll("\\s+", " ").strip();
                if (body.length() > 300) body = body.substring(0, 300) + "...";
                sb.append(body).append('\n');
            }
            sb.append('\n');
        }

        return sb.toString();
    }
}

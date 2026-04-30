package com.hefesto.testcases;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.hefesto.testcases.TestCaseEvidenceRepository.EvidenceContent;

/**
 * Gera relatório HTML print-friendly dos casos de teste de uma conversa.
 * Imagens são embutidas em base64 (data URI), permitindo "Salvar como PDF"
 * direto pelo navegador (Ctrl+P) sem depender de libs server-side.
 */
@Service
public class TestCaseReportService {

    private static final DateTimeFormatter BR_DATE = DateTimeFormatter
        .ofPattern("dd/MM/yyyy HH:mm", new Locale("pt", "BR"))
        .withZone(ZoneId.systemDefault());

    private final TestCaseRepository repo;
    private final TestCaseEvidenceRepository evidenceRepo;

    public TestCaseReportService(
        TestCaseRepository repo,
        TestCaseEvidenceRepository evidenceRepo
    ) {
        this.repo = repo;
        this.evidenceRepo = evidenceRepo;
    }

    public String renderForConversation(String conversationId, String conversationTitle) {
        List<TestCase> cases = repo.findByConversation(conversationId);
        return render(cases, "Conversa #" + conversationId, conversationTitle);
    }

    public String renderForMessage(String messageId, String conversationTitle) {
        List<TestCase> cases = repo.findByMessage(messageId);
        String header = cases.isEmpty()
            ? "Mensagem #" + messageId
            : "Mensagem #" + messageId + " (Conversa #" + cases.get(0).conversationId() + ")";
        return render(cases, header, conversationTitle);
    }

    // -----------------------------------------------------------------------

    private String render(List<TestCase> cases, String scopeLabel, String conversationTitle) {
        int total = cases.size();
        int passed = (int) cases.stream().filter(c -> "PASSED".equals(c.status())).count();
        int failed = (int) cases.stream().filter(c -> "FAILED".equals(c.status())).count();
        int blocked = (int) cases.stream().filter(c -> "BLOCKED".equals(c.status())).count();
        int pending = (int) cases.stream().filter(c -> "PENDING".equals(c.status())).count();

        StringBuilder html = new StringBuilder();
        html.append(htmlHeader(conversationTitle == null ? scopeLabel : conversationTitle));

        html.append("<header class=\"report-header\">\n");
        html.append("  <div class=\"report-title\">Relatório de Casos de Teste</div>\n");
        html.append("  <div class=\"report-subtitle\">").append(escape(conversationTitle == null ? scopeLabel : conversationTitle)).append("</div>\n");
        html.append("  <div class=\"report-meta\">\n");
        html.append("    <span><strong>Escopo:</strong> ").append(escape(scopeLabel)).append("</span>\n");
        html.append("    <span><strong>Gerado em:</strong> ").append(BR_DATE.format(Instant.now())).append("</span>\n");
        html.append("    <span><strong>Origem:</strong> Hefesto · QA Specialist</span>\n");
        html.append("  </div>\n");
        html.append("</header>\n");

        // Summary
        html.append("<section class=\"summary\">\n");
        html.append("  <div class=\"summary-title\">Resumo da Execução</div>\n");
        html.append("  <div class=\"summary-grid\">\n");
        appendKpi(html, "Total", String.valueOf(total), "neutral");
        appendKpi(html, "Aprovados", String.valueOf(passed), "passed");
        appendKpi(html, "Reprovados", String.valueOf(failed), "failed");
        appendKpi(html, "Bloqueados", String.valueOf(blocked), "blocked");
        appendKpi(html, "Pendentes", String.valueOf(pending), "pending");
        html.append("  </div>\n");
        html.append("</section>\n");

        if (cases.isEmpty()) {
            html.append("<div class=\"empty\">Nenhum caso de teste registrado neste escopo.</div>\n");
        } else {
            for (TestCase c : cases) {
                html.append(renderCase(c));
            }
        }

        html.append(htmlFooter());
        return html.toString();
    }

    private String renderCase(TestCase c) {
        StringBuilder sb = new StringBuilder();
        String statusKey = c.status() == null ? "PENDING" : c.status();
        String statusClass = statusKey.toLowerCase(Locale.ROOT);

        sb.append("<article class=\"case status-").append(statusClass).append("\">\n");

        // Header
        sb.append("  <header class=\"case-header\">\n");
        sb.append("    <div class=\"case-id\">").append(escape(c.code() == null ? "—" : c.code())).append("</div>\n");
        sb.append("    <div class=\"case-title\">").append(escape(c.title())).append("</div>\n");
        sb.append("    <div class=\"case-tags\">\n");
        if (c.category() != null) {
            sb.append("      <span class=\"tag tag-cat\">").append(escape(c.category())).append("</span>\n");
        }
        if (c.priority() != null) {
            sb.append("      <span class=\"tag tag-priority\">").append(escape(c.priority())).append("</span>\n");
        }
        sb.append("      <span class=\"tag tag-status status-").append(statusClass).append("\">").append(statusLabel(statusKey)).append("</span>\n");
        sb.append("    </div>\n");
        sb.append("  </header>\n");

        // Body
        sb.append("  <div class=\"case-body\">\n");

        if (notBlank(c.preconditions())) {
            sb.append("    <div class=\"case-section\"><div class=\"section-label\">Pré-condições</div>\n");
            sb.append("      <p>").append(escape(c.preconditions())).append("</p></div>\n");
        }

        if (c.steps() != null && !c.steps().isEmpty()) {
            sb.append("    <div class=\"case-section\"><div class=\"section-label\">Passos</div>\n      <ol>\n");
            for (String step : c.steps()) {
                sb.append("        <li>").append(escape(step)).append("</li>\n");
            }
            sb.append("      </ol></div>\n");
        }

        if (notBlank(c.expectedResult())) {
            sb.append("    <div class=\"case-section\"><div class=\"section-label\">Resultado esperado</div>\n");
            sb.append("      <p>").append(escape(c.expectedResult())).append("</p></div>\n");
        }

        if (notBlank(c.notes())) {
            sb.append("    <div class=\"case-section\"><div class=\"section-label\">Anotações da execução</div>\n");
            sb.append("      <p class=\"notes\">").append(escape(c.notes())).append("</p></div>\n");
        }

        // Evidences
        List<TestCaseEvidence> evidence = evidenceRepo.findByTestCase(c.id());
        if (!evidence.isEmpty()) {
            sb.append("    <div class=\"case-section\"><div class=\"section-label\">Evidências (").append(evidence.size()).append(")</div>\n");
            sb.append("      <div class=\"evidences\">\n");
            for (TestCaseEvidence ev : evidence) {
                sb.append(renderEvidence(ev));
            }
            sb.append("      </div></div>\n");
        }

        sb.append("  </div>\n");
        sb.append("</article>\n");
        return sb.toString();
    }

    private String renderEvidence(TestCaseEvidence ev) {
        StringBuilder sb = new StringBuilder();
        boolean isImage = ev.contentType() != null && ev.contentType().startsWith("image/");

        sb.append("<figure class=\"evidence\">\n");

        if (isImage) {
            // Embute imagem em base64
            EvidenceContent content = evidenceRepo.downloadById(ev.id()).orElse(null);
            if (content != null && content.content() != null) {
                String b64 = Base64.getEncoder().encodeToString(content.content());
                sb.append("  <img src=\"data:").append(escape(ev.contentType())).append(";base64,").append(b64)
                  .append("\" alt=\"").append(escape(ev.filename())).append("\" />\n");
            } else {
                sb.append("  <div class=\"evidence-missing\">[imagem não disponível]</div>\n");
            }
        } else {
            // Não-imagem: ícone + nome
            sb.append("  <div class=\"evidence-file\">\n");
            sb.append("    <div class=\"file-icon\">📎</div>\n");
            sb.append("    <div class=\"file-name\">").append(escape(ev.filename())).append("</div>\n");
            sb.append("    <div class=\"file-size\">").append(formatBytes(ev.sizeBytes())).append("</div>\n");
            sb.append("  </div>\n");
        }

        sb.append("  <figcaption>\n");
        sb.append("    <strong>").append(escape(ev.filename())).append("</strong>\n");
        sb.append("    <span class=\"evidence-meta\">").append(formatBytes(ev.sizeBytes()));
        sb.append(" · ").append(BR_DATE.format(Instant.ofEpochMilli(ev.uploadedAt()))).append("</span>\n");
        if (notBlank(ev.note())) {
            sb.append("    <p class=\"evidence-note\">").append(escape(ev.note())).append("</p>\n");
        }
        sb.append("  </figcaption>\n");
        sb.append("</figure>\n");
        return sb.toString();
    }

    private static void appendKpi(StringBuilder html, String label, String value, String variant) {
        html.append("    <div class=\"kpi kpi-").append(variant).append("\">\n");
        html.append("      <div class=\"kpi-value\">").append(value).append("</div>\n");
        html.append("      <div class=\"kpi-label\">").append(escape(label)).append("</div>\n");
        html.append("    </div>\n");
    }

    private static String statusLabel(String status) {
        return switch (status) {
            case "PASSED"  -> "Aprovado";
            case "FAILED"  -> "Reprovado";
            case "BLOCKED" -> "Bloqueado";
            case "PENDING" -> "Pendente";
            default        -> status;
        };
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private static String htmlHeader(String pageTitle) {
        return """
            <!doctype html>
            <html lang="pt-BR">
            <head>
              <meta charset="utf-8" />
              <title>Relatório de Casos de Teste — """ + escape(pageTitle) + """
            </title>
              <style>
                * { box-sizing: border-box; }
                body {
                  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                  max-width: 920px;
                  margin: 32px auto;
                  padding: 0 24px 64px;
                  color: #1a2332;
                  background: #ffffff;
                  line-height: 1.5;
                  font-size: 14px;
                }
                .report-header {
                  border-bottom: 3px solid #0090c0;
                  padding-bottom: 18px;
                  margin-bottom: 28px;
                }
                .report-title {
                  font-size: 28px;
                  font-weight: 600;
                  color: #0a1a2c;
                  letter-spacing: -0.01em;
                  margin-bottom: 4px;
                }
                .report-subtitle {
                  font-size: 16px;
                  color: #4a5878;
                  margin-bottom: 12px;
                }
                .report-meta {
                  display: flex;
                  flex-wrap: wrap;
                  gap: 18px;
                  font-size: 12px;
                  color: #6b7a99;
                }
                .summary {
                  background: #f5f7fb;
                  border: 1px solid #d0d6e0;
                  padding: 18px 20px;
                  margin-bottom: 28px;
                }
                .summary-title {
                  font-size: 13px;
                  font-weight: 600;
                  text-transform: uppercase;
                  letter-spacing: 0.08em;
                  color: #4a5878;
                  margin-bottom: 14px;
                }
                .summary-grid {
                  display: grid;
                  grid-template-columns: repeat(5, 1fr);
                  gap: 12px;
                }
                .kpi {
                  text-align: center;
                  padding: 12px 8px;
                  background: #ffffff;
                  border: 1px solid #d0d6e0;
                }
                .kpi-value {
                  font-size: 28px;
                  font-weight: 700;
                  line-height: 1;
                  color: #1a2332;
                }
                .kpi-label {
                  font-size: 10px;
                  text-transform: uppercase;
                  letter-spacing: 0.1em;
                  color: #6b7a99;
                  margin-top: 6px;
                }
                .kpi-passed .kpi-value  { color: #0d9b6c; }
                .kpi-failed .kpi-value  { color: #d93b69; }
                .kpi-blocked .kpi-value { color: #d68a30; }
                .kpi-pending .kpi-value { color: #6b7a99; }
                .kpi-neutral .kpi-value { color: #1a2332; }
                .case {
                  border: 1px solid #d0d6e0;
                  margin-bottom: 24px;
                  page-break-inside: avoid;
                  break-inside: avoid;
                }
                .case-header {
                  display: flex;
                  align-items: center;
                  gap: 12px;
                  padding: 12px 16px;
                  background: #f5f7fb;
                  border-bottom: 1px solid #d0d6e0;
                  flex-wrap: wrap;
                }
                .case-id {
                  font-family: 'JetBrains Mono', monospace;
                  font-weight: 700;
                  font-size: 13px;
                  color: #0090c0;
                }
                .case-title {
                  flex: 1;
                  font-size: 14px;
                  font-weight: 500;
                  color: #1a2332;
                  min-width: 200px;
                }
                .case-tags {
                  display: flex;
                  gap: 6px;
                  flex-wrap: wrap;
                }
                .tag {
                  font-size: 10px;
                  text-transform: uppercase;
                  letter-spacing: 0.08em;
                  padding: 2px 8px;
                  border: 1px solid #d0d6e0;
                  font-weight: 500;
                }
                .tag-status.status-passed   { color: #0d9b6c; border-color: #0d9b6c; }
                .tag-status.status-failed   { color: #d93b69; border-color: #d93b69; }
                .tag-status.status-blocked  { color: #d68a30; border-color: #d68a30; }
                .tag-status.status-pending  { color: #6b7a99; border-color: #6b7a99; }
                .tag-cat                    { color: #4a5878; }
                .tag-priority               { color: #6b7a99; }
                .case-body {
                  padding: 16px 18px;
                }
                .case-section {
                  margin-bottom: 14px;
                }
                .case-section:last-child { margin-bottom: 0; }
                .section-label {
                  font-size: 11px;
                  text-transform: uppercase;
                  letter-spacing: 0.1em;
                  color: #6b7a99;
                  font-weight: 600;
                  margin-bottom: 6px;
                }
                .case-body p { margin: 0; }
                .case-body ol { margin: 0; padding-left: 20px; }
                .case-body li { margin-bottom: 4px; }
                .notes {
                  background: #fff7ed;
                  border-left: 3px solid #d68a30;
                  padding: 8px 12px;
                  font-size: 13px;
                }
                .evidences {
                  display: grid;
                  grid-template-columns: 1fr;
                  gap: 16px;
                }
                .evidence {
                  margin: 0;
                  border: 1px solid #d0d6e0;
                  padding: 8px;
                  background: #ffffff;
                }
                .evidence img {
                  max-width: 100%;
                  max-height: 480px;
                  display: block;
                  margin: 0 auto;
                }
                .evidence-file {
                  display: flex;
                  align-items: center;
                  gap: 12px;
                  padding: 12px;
                  background: #f5f7fb;
                }
                .file-icon { font-size: 24px; }
                .file-name { flex: 1; font-weight: 500; }
                .file-size { font-size: 11px; color: #6b7a99; }
                .evidence-missing { color: #d93b69; font-style: italic; }
                .evidence figcaption {
                  margin-top: 6px;
                  font-size: 12px;
                  color: #4a5878;
                  display: flex;
                  flex-wrap: wrap;
                  gap: 8px;
                  align-items: baseline;
                }
                .evidence-meta { color: #6b7a99; }
                .evidence-note {
                  width: 100%;
                  margin-top: 4px;
                  font-style: italic;
                  color: #4a5878;
                }
                .empty {
                  text-align: center;
                  padding: 48px 16px;
                  color: #6b7a99;
                  font-style: italic;
                }
                @media print {
                  body { margin: 0; padding: 16px 24px; max-width: none; }
                  .case { page-break-inside: avoid; }
                  .summary, .case { box-shadow: none; }
                  a { color: inherit; text-decoration: none; }
                }
              </style>
            </head>
            <body>
            """;
    }

    private static String htmlFooter() {
        return """
            <footer style="margin-top:40px;border-top:1px solid #d0d6e0;padding-top:14px;font-size:11px;color:#6b7a99;text-align:center">
              Hefesto · Relatório gerado automaticamente · Use Ctrl+P (ou Cmd+P) para salvar como PDF.
            </footer>
            </body>
            </html>
            """;
    }
}

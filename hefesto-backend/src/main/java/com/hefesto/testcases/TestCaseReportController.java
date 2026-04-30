package com.hefesto.testcases;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hefesto.chat.Conversation;
import com.hefesto.chat.ConversationStore;

/**
 * Endpoints que retornam relatórios de casos de teste em HTML, prontos
 * pra serem visualizados no navegador e exportados como PDF (Ctrl+P).
 *
 * <p>Imagens das evidências são embutidas em base64, então o PDF gerado
 * pelo navegador inclui tudo num arquivo único — sem dependências externas
 * nem links quebrados.</p>
 */
@RestController
@RequestMapping("/api/reports")
public class TestCaseReportController {

    private final TestCaseReportService report;
    private final ConversationStore conversations;

    public TestCaseReportController(
        TestCaseReportService report,
        ConversationStore conversations
    ) {
        this.report = report;
        this.conversations = conversations;
    }

    /**
     * Relatório de todos os casos de teste de uma conversa.
     *
     * @param download se true, força o navegador a baixar como arquivo
     *                 (Content-Disposition: attachment) em vez de exibir.
     */
    @GetMapping("/test-cases")
    public ResponseEntity<String> reportByConversation(
        @RequestParam("conversationId") String conversationId,
        @RequestParam(value = "download", required = false, defaultValue = "false") boolean download
    ) {
        Conversation c = conversations.get(conversationId);
        String title = c != null && c.title() != null ? c.title() : "Conversa " + conversationId;
        String html = report.renderForConversation(conversationId, title);

        return responseFor(html, "casos-de-teste-" + conversationId + ".html", download);
    }

    /**
     * Relatório dos casos extraídos de uma mensagem específica.
     */
    @GetMapping("/test-cases-by-message")
    public ResponseEntity<String> reportByMessage(
        @RequestParam("messageId") String messageId,
        @RequestParam(value = "download", required = false, defaultValue = "false") boolean download
    ) {
        String html = report.renderForMessage(messageId, null);
        return responseFor(html, "casos-de-teste-msg-" + messageId + ".html", download);
    }

    private ResponseEntity<String> responseFor(String html, String filename, boolean download) {
        String disposition = (download ? "attachment" : "inline")
            + "; filename=\"" + filename + "\"";
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
            // Cache curto pra refletir mudanças de status sem stale.
            .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
            .body(html);
    }
}

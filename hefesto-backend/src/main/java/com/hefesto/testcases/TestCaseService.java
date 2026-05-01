package com.hefesto.testcases;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hefesto.agents.Agent;
import com.hefesto.agents.AgentRegistry;
import com.hefesto.testcases.TestCaseExtractor.ParsedTestCase;

/**
 * Orquestra extração + persistência de test cases. Chamado pelo
 * ChatService/WebSocketHandler quando o agente QA Specialist responde.
 */
@Service
public class TestCaseService {

    private static final Logger log = LoggerFactory.getLogger(TestCaseService.class);

    private final TestCaseExtractor extractor;
    private final TestCaseRepository repo;
    private final TestCaseEvidenceRepository evidenceRepo;
    private final AgentRegistry agents;

    public TestCaseService(
        TestCaseExtractor extractor,
        TestCaseRepository repo,
        TestCaseEvidenceRepository evidenceRepo,
        AgentRegistry agents
    ) {
        this.extractor = extractor;
        this.repo = repo;
        this.evidenceRepo = evidenceRepo;
        this.agents = agents;
    }

    /**
     * Extrai casos de uma resposta de assistant e persiste. Só extrai se o
     * agente for o QA Specialist (evita ruído com agentes que não geram
     * casos formatados). Falhas são logadas mas não propagam.
     *
     * @return lista de casos persistidos (vazia se nada extraído).
     */
    public List<TestCase> extractAndSave(
        String agentId,
        String conversationId,
        String messageId,
        String assistantContent
    ) {
        if (assistantContent == null || assistantContent.isBlank()) return List.of();
        // Só extrai se o agente atual estiver marcado pra isso
        // (frontmatter "extractsTestCases: true" no .md).
        Agent agent = agents.getOrDefault(agentId);
        if (agent == null || !agent.extractsTestCases()) return List.of();

        try {
            List<ParsedTestCase> parsed = extractor.extract(assistantContent);
            if (parsed.isEmpty()) {
                log.debug("Nenhum test case extraído da resposta do conv={}", conversationId);
                return List.of();
            }

            List<TestCase> saved = new ArrayList<>();
            for (ParsedTestCase p : parsed) {
                TestCase tc = new TestCase(
                    null,                  // id gerado no save
                    conversationId,
                    messageId,
                    p.code(),
                    p.category(),
                    p.title(),
                    p.preconditions(),
                    p.steps(),
                    p.expectedResult(),
                    p.priority(),
                    TestCase.STATUS_PENDING,
                    null,
                    p.position(),
                    0,                      // created_at será preenchido pelo repo
                    0
                );
                saved.add(repo.save(tc));
            }
            log.info("Extraídos {} test cases da conversa {} (message {})",
                saved.size(), conversationId, messageId);
            return saved;
        } catch (Exception e) {
            log.warn("Falha ao extrair test cases da conv={}: {}", conversationId, e.getMessage());
            return List.of();
        }
    }

    public List<TestCase> findByConversation(String conversationId) {
        return repo.findByConversation(conversationId);
    }

    public List<TestCase> findByMessage(String messageId) {
        return repo.findByMessage(messageId);
    }

    public Optional<TestCase> findById(String id) {
        return repo.findById(id);
    }

    public void updateStatus(String id, String status, String notes) {
        if (!isValidStatus(status)) {
            throw new IllegalArgumentException("Status inválido: " + status);
        }
        repo.updateStatus(id, status, notes);
    }

    public void delete(String id) {
        repo.delete(id);
    }

    // ---- Evidence ----

    public TestCaseEvidence addEvidence(
        String testCaseId,
        String filename,
        String contentType,
        byte[] content,
        String note
    ) {
        if (repo.findById(testCaseId).isEmpty()) {
            throw new IllegalArgumentException("Test case não encontrado: " + testCaseId);
        }
        return evidenceRepo.save(testCaseId, filename, contentType, content, note);
    }

    public List<TestCaseEvidence> evidenceOf(String testCaseId) {
        return evidenceRepo.findByTestCase(testCaseId);
    }

    public Optional<TestCaseEvidenceRepository.EvidenceContent> downloadEvidence(String evidenceId) {
        return evidenceRepo.downloadById(evidenceId);
    }

    public boolean deleteEvidence(String evidenceId) {
        return evidenceRepo.delete(evidenceId);
    }

    private static boolean isValidStatus(String status) {
        return TestCase.STATUS_PENDING.equals(status)
            || TestCase.STATUS_PASSED.equals(status)
            || TestCase.STATUS_FAILED.equals(status)
            || TestCase.STATUS_BLOCKED.equals(status);
    }

    /** Pra futuras integrações que precisem checar se um agente arbitrário gera cases. */
    public static boolean isExtractingAgent(Agent agent) {
        return agent != null && agent.extractsTestCases();
    }
}

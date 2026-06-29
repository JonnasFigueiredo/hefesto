package com.hefesto.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.hefesto.testcases.TestCase;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Cobre a formatação da descrição da subtarefa a partir de um caso de teste
 * (usada por create_test_subtasks).
 */
class WorkflowMcpToolsTest {

    private static TestCase tc() {
        return new TestCase(
                "id1", "conv1", "msg1", "TC-001", "POSITIVO", "Login com Google válido",
                "Usuário possui conta Google",
                List.of("Acessar tela de login", "Clicar em 'Entrar com Google'", "Autorizar"),
                "Usuário é autenticado e redirecionado",
                "P1", TestCase.STATUS_PENDING, null, 0, 0L, 0L);
    }

    @Test
    void formatTestCase_includesCategoryStepsAndExpected() {
        String out = WorkflowMcpTools.formatTestCase(tc());
        assertThat(out).contains("Categoria: POSITIVO");
        assertThat(out).contains("Prioridade: P1");
        assertThat(out).contains("Pré-condições:");
        assertThat(out).contains("1. Acessar tela de login");
        assertThat(out).contains("3. Autorizar");
        assertThat(out).contains("Resultado esperado:");
        assertThat(out).contains("Usuário é autenticado e redirecionado");
    }

    @Test
    void formatTestCase_handlesMinimalCase() {
        TestCase minimal = new TestCase(
                "id", "c", "m", "TC-002", null, "Caso mínimo",
                null, List.of(), null, null, TestCase.STATUS_PENDING, null, 1, 0L, 0L);
        // Não deve lançar e deve devolver algo coerente (mesmo que curto).
        String out = WorkflowMcpTools.formatTestCase(minimal);
        assertThat(out).isNotNull();
    }
}

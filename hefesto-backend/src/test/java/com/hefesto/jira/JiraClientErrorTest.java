package com.hefesto.jira;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Cobre a extração de mensagem de erro do Jira — em especial o objeto
 * {@code errors} (campo→motivo) que antes virava só "errors" e impedia
 * diagnosticar 400 de criação (ex: issuetype inválido).
 */
class JiraClientErrorTest {

    @Test
    void surfacesErrorMessagesArray() {
        String body = "{\"errorMessages\":[\"Issue does not exist\"],\"errors\":{}}";
        assertThat(JiraClient.extractMessage(body)).isEqualTo("Issue does not exist");
    }

    @Test
    void surfacesFieldErrorsObject() {
        String body = "{\"errorMessages\":[],\"errors\":{\"issuetype\":\"valid issue type is required\"}}";
        assertThat(JiraClient.extractMessage(body)).contains("issuetype: valid issue type is required");
    }

    @Test
    void combinesBothSources() {
        String body = "{\"errorMessages\":[\"geral\"],\"errors\":{\"summary\":\"obrigatório\"}}";
        String msg = JiraClient.extractMessage(body);
        assertThat(msg).contains("geral").contains("summary: obrigatório");
    }

    @Test
    void nonJsonBodyFallsBackToTruncatedText() {
        assertThat(JiraClient.extractMessage("<html>500</html>")).isEqualTo("<html>500</html>");
    }

    @Test
    void blankBody() {
        assertThat(JiraClient.extractMessage("")).isEqualTo("(sem corpo)");
    }
}

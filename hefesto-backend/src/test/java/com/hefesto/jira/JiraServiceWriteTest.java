package com.hefesto.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hefesto.jira.dto.CreatedIssueDto;

/**
 * Testa a camada de escrita do {@link JiraService} mockando o {@link JiraClient}:
 * valida os payloads montados sem bater na API real.
 */
@ExtendWith(MockitoExtension.class)
class JiraServiceWriteTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    JiraClient client;

    JiraService service;

    @BeforeEach
    void setup() {
        service = new JiraService(client, new JiraProperties("https://acme.atlassian.net", "e@e.com", "tok"));
    }

    private static JsonNode json(String s) {
        try {
            return MAPPER.readTree(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void createStory_buildsProjectIssuetypeSummaryDescription_andReturnsKeyAndUrl() {
        when(client.createIssue(any())).thenReturn(json("{\"id\":\"10001\",\"key\":\"PROJ-1\"}"));

        CreatedIssueDto created = service.createStory(
                "PROJ", "Story", "Login social", "Permitir login via Google",
                List.of("Dado um usuário Google", "Então autentica"));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(client).createIssue(captor.capture());
        Map<String, Object> fields = captor.getValue();

        assertThat(((Map<String, Object>) fields.get("project")).get("key")).isEqualTo("PROJ");
        assertThat(((Map<String, Object>) fields.get("issuetype")).get("name")).isEqualTo("Story");
        assertThat(fields.get("summary")).isEqualTo("Login social");
        assertThat(fields.get("description")).isInstanceOf(Map.class);

        assertThat(created.key()).isEqualTo("PROJ-1");
        assertThat(created.url()).isEqualTo("https://acme.atlassian.net/browse/PROJ-1");
    }

    @Test
    void createStory_defaultsIssueTypeToTaskWhenBlank() {
        when(client.createIssue(any())).thenReturn(json("{\"key\":\"PROJ-2\"}"));
        service.createStory("PROJ", "  ", "t", "d", null);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(client).createIssue(captor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> issuetype = (Map<String, Object>) captor.getValue().get("issuetype");
        assertThat(issuetype.get("name")).isEqualTo("Task");
    }

    @SuppressWarnings("unchecked")
    @Test
    void createSubtask_derivesProjectFromParentAndSetsParent() {
        when(client.createIssue(any())).thenReturn(json("{\"key\":\"PROJ-124\"}"));

        service.createSubtask("PROJ-123", null, "TC-001 verifica login", "passos...");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(client).createIssue(captor.capture());
        Map<String, Object> fields = captor.getValue();

        assertThat(((Map<String, Object>) fields.get("project")).get("key")).isEqualTo("PROJ");
        assertThat(((Map<String, Object>) fields.get("parent")).get("key")).isEqualTo("PROJ-123");
        assertThat(((Map<String, Object>) fields.get("issuetype")).get("name")).isEqualTo("Sub-task");
    }

    @Test
    void createSubtask_rejectsParentKeyWithoutDash() {
        assertThatThrownBy(() -> service.createSubtask("PROJ", null, "x", "y"))
                .isInstanceOf(JiraApiException.class);
        verify(client, never()).createIssue(any());
    }

    @Test
    void updateIssue_onlySendsProvidedFields() {
        service.updateIssue("PROJ-1", "Novo título", null, null);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(client).updateIssue(eq("PROJ-1"), captor.capture());
        Map<String, Object> fields = captor.getValue();
        assertThat(fields).containsKey("summary");
        assertThat(fields).doesNotContainKey("description");
    }

    @Test
    void updateIssue_rejectsWhenNothingToUpdate() {
        assertThatThrownBy(() -> service.updateIssue("PROJ-1", null, null, null))
                .isInstanceOf(JiraApiException.class);
        verify(client, never()).updateIssue(any(), any());
    }

    @Test
    void transitionIssue_resolvesNameToIdCaseInsensitive() {
        when(client.getTransitions("PROJ-1")).thenReturn(json(
                "{\"transitions\":[{\"id\":\"11\",\"name\":\"To Do\"},{\"id\":\"31\",\"name\":\"Done\"}]}"));

        service.transitionIssue("PROJ-1", "done");

        verify(client).transitionIssue("PROJ-1", "31");
    }

    @Test
    void transitionIssue_unknownNameThrowsWithOptions() {
        when(client.getTransitions("PROJ-1")).thenReturn(json(
                "{\"transitions\":[{\"id\":\"11\",\"name\":\"To Do\"}]}"));

        assertThatThrownBy(() -> service.transitionIssue("PROJ-1", "Done"))
                .isInstanceOf(JiraApiException.class)
                .hasMessageContaining("To Do");
        verify(client, never()).transitionIssue(any(), any());
    }
}

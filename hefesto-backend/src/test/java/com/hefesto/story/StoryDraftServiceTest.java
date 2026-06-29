package com.hefesto.story;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Cobre o parser de rascunho — precisa ser robusto a JSON em cerca ```json,
 * texto ao redor, e ausência de JSON (fallback).
 */
class StoryDraftServiceTest {

    @Test
    void parsesFencedJson() {
        String resp = "Claro! Aqui está:\n```json\n"
                + "{\"summary\":\"Login social\",\"description\":\"Permitir Google\","
                + "\"acceptanceCriteria\":[\"Dado X\",\"Então Y\"]}\n```\nEspero ter ajudado.";
        StoryDraft d = StoryDraftService.parse(resp);
        assertThat(d.summary()).isEqualTo("Login social");
        assertThat(d.description()).isEqualTo("Permitir Google");
        assertThat(d.acceptanceCriteria()).containsExactly("Dado X", "Então Y");
    }

    @Test
    void parsesBareJsonObject() {
        String resp = "{\"summary\":\"A\",\"description\":\"B\",\"acceptanceCriteria\":[]}";
        StoryDraft d = StoryDraftService.parse(resp);
        assertThat(d.summary()).isEqualTo("A");
        assertThat(d.description()).isEqualTo("B");
        assertThat(d.acceptanceCriteria()).isEmpty();
    }

    @Test
    void parsesJsonSurroundedByProse() {
        String resp = "Segue a história:\n{\"summary\":\"X\",\"description\":\"Y\",\"acceptanceCriteria\":[\"c1\"]}\nFim.";
        StoryDraft d = StoryDraftService.parse(resp);
        assertThat(d.summary()).isEqualTo("X");
        assertThat(d.acceptanceCriteria()).containsExactly("c1");
    }

    @Test
    void fallsBackToDescriptionWhenNoJson() {
        String resp = "Não consegui estruturar, mas a ideia é permitir login.";
        StoryDraft d = StoryDraftService.parse(resp);
        assertThat(d.summary()).isEmpty();
        assertThat(d.description()).isEqualTo(resp);
        assertThat(d.acceptanceCriteria()).isEmpty();
    }

    @Test
    void ignoresBlankCriteria() {
        String resp = "{\"summary\":\"A\",\"description\":\"B\",\"acceptanceCriteria\":[\"ok\",\"\",\"  \"]}";
        StoryDraft d = StoryDraftService.parse(resp);
        assertThat(d.acceptanceCriteria()).containsExactly("ok");
    }
}

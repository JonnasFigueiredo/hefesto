package com.hefesto.jira;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Garante que o builder de ADF produz a estrutura mínima que a Jira Cloud v3
 * aceita em description/comment.
 */
class AdfTest {

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> contentOf(Map<String, Object> doc) {
        return (List<Map<String, Object>>) doc.get("content");
    }

    @Test
    void fromText_isValidDocWithVersion() {
        Map<String, Object> doc = Adf.fromText("linha um");
        assertThat(doc).containsEntry("type", "doc").containsEntry("version", 1);
        assertThat(contentOf(doc)).isNotEmpty();
    }

    @Test
    void fromText_skipsBlankLinesAndMakesOneParagraphPerLine() {
        Map<String, Object> doc = Adf.fromText("um\n\n  \ndois");
        List<Map<String, Object>> content = contentOf(doc);
        assertThat(content).hasSize(2);
        assertThat(content).allSatisfy(p -> assertThat(p).containsEntry("type", "paragraph"));
    }

    @Test
    void fromText_emptyInputStillProducesAtLeastOneNode() {
        // ADF não aceita doc com content vazio; deve cair num parágrafo vazio.
        assertThat(contentOf(Adf.fromText(""))).hasSize(1);
        assertThat(contentOf(Adf.fromText(null))).hasSize(1);
    }

    @Test
    void paragraph_withText_hasTextNode() {
        Map<String, Object> p = Adf.paragraph("oi");
        assertThat(p).containsEntry("type", "paragraph");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inner = (List<Map<String, Object>>) p.get("content");
        assertThat(inner).hasSize(1);
        assertThat(inner.get(0)).containsEntry("type", "text").containsEntry("text", "oi");
    }

    @Test
    void paragraph_empty_hasNoTextNode() {
        // Node "text" com string vazia é inválido no ADF; parágrafo vazio = content [].
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> inner = (List<Map<String, Object>>) Adf.paragraph("").get("content");
        assertThat(inner).isEmpty();
    }

    @Test
    void descriptionWithCriteria_appendsHeadingAndBulletList() {
        Map<String, Object> doc = Adf.descriptionWithCriteria(
                "Descrição da história", List.of("Critério 1", "Critério 2"));
        List<Map<String, Object>> content = contentOf(doc);

        // parágrafo + heading + bulletList
        assertThat(content).anySatisfy(n -> assertThat(n).containsEntry("type", "heading"));
        Map<String, Object> bulletList = content.stream()
                .filter(n -> "bulletList".equals(n.get("type"))).findFirst().orElseThrow();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) bulletList.get("content");
        assertThat(items).hasSize(2);
        assertThat(items).allSatisfy(i -> assertThat(i).containsEntry("type", "listItem"));
    }

    @Test
    void descriptionWithCriteria_withoutCriteria_hasNoBulletList() {
        Map<String, Object> doc = Adf.descriptionWithCriteria("só descrição", List.of());
        assertThat(contentOf(doc)).noneSatisfy(n -> assertThat(n).containsEntry("type", "bulletList"));
    }
}

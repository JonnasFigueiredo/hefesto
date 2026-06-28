package com.hefesto.jira;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder mínimo de ADF (Atlassian Document Format) — o formato que a Jira
 * Cloud REST API v3 exige nos campos {@code description} e em comentários.
 *
 * <p>Suporta só o necessário pro fluxo do Hefesto: parágrafos (um por linha
 * não-vazia) e listas com marcadores. É o caminho inverso de
 * {@link AdfToMarkdown} (que lê ADF→markdown).</p>
 */
public final class Adf {

    private Adf() {}

    /** Documento ADF a partir de texto livre (cada linha não-vazia vira parágrafo). */
    public static Map<String, Object> fromText(String text) {
        List<Map<String, Object>> content = new ArrayList<>();
        if (text != null) {
            for (String line : text.split("\r?\n")) {
                if (!line.isBlank()) content.add(paragraph(line.strip()));
            }
        }
        if (content.isEmpty()) content.add(paragraph(""));
        return doc(content);
    }

    /**
     * Documento ADF com uma descrição livre seguida (opcionalmente) de uma
     * seção "Critérios de aceite" como bullet list.
     */
    public static Map<String, Object> descriptionWithCriteria(String description, List<String> criteria) {
        List<Map<String, Object>> content = new ArrayList<>();
        if (description != null) {
            for (String line : description.split("\r?\n")) {
                if (!line.isBlank()) content.add(paragraph(line.strip()));
            }
        }
        if (criteria != null && !criteria.isEmpty()) {
            content.add(heading("Critérios de aceite", 3));
            content.add(bulletList(criteria));
        }
        if (content.isEmpty()) content.add(paragraph(""));
        return doc(content);
    }

    public static Map<String, Object> doc(List<Map<String, Object>> content) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("type", "doc");
        doc.put("version", 1);
        doc.put("content", content);
        return doc;
    }

    public static Map<String, Object> paragraph(String text) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", "paragraph");
        // Parágrafo vazio: content vazio (ADF aceita); texto vazio em node text não.
        if (text == null || text.isEmpty()) {
            p.put("content", List.of());
        } else {
            p.put("content", List.of(textNode(text)));
        }
        return p;
    }

    public static Map<String, Object> heading(String text, int level) {
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("type", "heading");
        h.put("attrs", Map.of("level", level));
        h.put("content", List.of(textNode(text)));
        return h;
    }

    public static Map<String, Object> bulletList(List<String> items) {
        List<Map<String, Object>> listItems = new ArrayList<>();
        for (String item : items) {
            listItems.add(Map.of(
                "type", "listItem",
                "content", List.of(paragraph(item == null ? "" : item.strip()))
            ));
        }
        Map<String, Object> list = new LinkedHashMap<>();
        list.put("type", "bulletList");
        list.put("content", listItems);
        return list;
    }

    private static Map<String, Object> textNode(String text) {
        return Map.of("type", "text", "text", text);
    }
}

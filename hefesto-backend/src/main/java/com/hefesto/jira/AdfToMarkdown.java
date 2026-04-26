package com.hefesto.jira;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Conversor minimalista de ADF (Atlassian Document Format) para Markdown.
 *
 * <p>Cobre os nós mais comuns em descrições/comentários do Jira:
 * paragraph, heading, bulletList, orderedList, listItem, codeBlock,
 * blockquote, hardBreak, rule, text com marks (strong, em, code, link).</p>
 *
 * <p>Nós desconhecidos são percorridos recursivamente extraindo apenas o
 * texto. Não falha em ADF malformado — emite o que conseguir.</p>
 */
public final class AdfToMarkdown {

    private AdfToMarkdown() {}

    public static String convert(JsonNode adf) {
        if (adf == null || adf.isNull() || adf.isMissingNode()) return "";
        StringBuilder out = new StringBuilder();
        renderNode(adf, out, 0);
        return out.toString().strip();
    }

    private static void renderNode(JsonNode node, StringBuilder out, int listDepth) {
        if (node == null || node.isNull()) return;
        String type = node.path("type").asText("");

        switch (type) {
            case "doc" -> renderChildren(node, out, listDepth);

            case "paragraph" -> {
                renderChildren(node, out, listDepth);
                out.append("\n\n");
            }

            case "heading" -> {
                int level = Math.min(6, Math.max(1, node.path("attrs").path("level").asInt(1)));
                out.append("#".repeat(level)).append(' ');
                renderChildren(node, out, listDepth);
                out.append("\n\n");
            }

            case "bulletList" -> {
                JsonNode content = node.path("content");
                if (content.isArray()) {
                    for (JsonNode item : content) {
                        out.append("  ".repeat(listDepth)).append("- ");
                        renderListItem(item, out, listDepth + 1);
                    }
                }
                out.append('\n');
            }

            case "orderedList" -> {
                JsonNode content = node.path("content");
                int i = 1;
                if (content.isArray()) {
                    for (JsonNode item : content) {
                        out.append("  ".repeat(listDepth)).append(i++).append(". ");
                        renderListItem(item, out, listDepth + 1);
                    }
                }
                out.append('\n');
            }

            case "listItem" -> renderChildren(node, out, listDepth);

            case "codeBlock" -> {
                String lang = node.path("attrs").path("language").asText("");
                out.append("```").append(lang).append('\n');
                renderChildren(node, out, listDepth);
                if (out.charAt(out.length() - 1) != '\n') out.append('\n');
                out.append("```\n\n");
            }

            case "blockquote" -> {
                StringBuilder inner = new StringBuilder();
                renderChildren(node, inner, listDepth);
                String[] lines = inner.toString().split("\n");
                for (String line : lines) {
                    out.append("> ").append(line).append('\n');
                }
                out.append('\n');
            }

            case "rule" -> out.append("\n---\n\n");

            case "hardBreak" -> out.append("  \n");

            case "text" -> renderText(node, out);

            case "mention" -> {
                String name = node.path("attrs").path("text").asText("");
                if (name.isBlank()) name = node.path("attrs").path("displayName").asText("@user");
                out.append("**").append(name).append("**");
            }

            case "emoji" -> {
                String shortName = node.path("attrs").path("shortName").asText("");
                String text = node.path("attrs").path("text").asText(shortName);
                out.append(text);
            }

            case "inlineCard", "media", "mediaSingle", "mediaGroup" -> {
                String url = node.path("attrs").path("url").asText("");
                if (!url.isBlank()) out.append('[').append(url).append("](").append(url).append(')');
            }

            case "table" -> renderChildren(node, out, listDepth);
            case "tableRow", "tableHeader", "tableCell" -> {
                renderChildren(node, out, listDepth);
                out.append(" | ");
            }

            default -> renderChildren(node, out, listDepth);
        }
    }

    private static void renderListItem(JsonNode item, StringBuilder out, int childDepth) {
        StringBuilder inner = new StringBuilder();
        renderChildren(item, inner, childDepth);
        // remove trailing newlines do paragraph dentro do item
        String s = inner.toString().strip();
        out.append(s).append('\n');
    }

    private static void renderChildren(JsonNode node, StringBuilder out, int listDepth) {
        JsonNode content = node.path("content");
        if (!content.isArray()) return;
        for (JsonNode child : content) {
            renderNode(child, out, listDepth);
        }
    }

    private static void renderText(JsonNode textNode, StringBuilder out) {
        String text = textNode.path("text").asText("");
        if (text.isEmpty()) return;

        boolean strong = false;
        boolean em = false;
        boolean code = false;
        boolean strike = false;
        String linkHref = null;

        JsonNode marks = textNode.path("marks");
        if (marks.isArray()) {
            for (JsonNode mark : marks) {
                String mt = mark.path("type").asText("");
                switch (mt) {
                    case "strong" -> strong = true;
                    case "em" -> em = true;
                    case "code" -> code = true;
                    case "strike" -> strike = true;
                    case "link" -> linkHref = mark.path("attrs").path("href").asText(null);
                    default -> { /* ignore */ }
                }
            }
        }

        String rendered = text;
        if (code) rendered = "`" + rendered + "`";
        if (strong) rendered = "**" + rendered + "**";
        if (em) rendered = "*" + rendered + "*";
        if (strike) rendered = "~~" + rendered + "~~";
        if (linkHref != null && !linkHref.isBlank()) {
            rendered = "[" + rendered + "](" + linkHref + ")";
        }

        out.append(rendered);
    }
}

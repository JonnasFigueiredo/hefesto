package com.hefesto.agents;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser de arquivos .md de agentes. Cada arquivo tem (opcionalmente) um
 * bloco de YAML frontmatter delimitado por {@code ---}, seguido pelo
 * system prompt em markdown.
 *
 * <p>Frontmatter suportado:</p>
 * <pre>
 * ---
 * description: "Designer de casos de teste"
 * emoji: "🧪"
 * defaultPromptTemplate: "Analise o contexto anexado..."
 * extractsTestCases: true
 * ---
 *
 * Você é um QA Specialist sênior...
 * </pre>
 *
 * <p>Se não houver frontmatter, o arquivo inteiro é o system prompt e os
 * metadados ficam vazios (com defaults razoáveis).</p>
 */
public final class AgentFileParser {

    private static final Pattern FRONTMATTER = Pattern.compile(
        "\\A---\\s*\\n(.*?)\\n---\\s*\\n?(.*)",
        Pattern.DOTALL);

    private AgentFileParser() {}

    /**
     * Parseia um arquivo de agente.
     *
     * @param filename  nome completo do arquivo (ex: "QAseniorAgent.md").
     * @param content   conteúdo bruto do arquivo (UTF-8).
     */
    public static Agent parse(String filename, String content) {
        if (filename == null) filename = "Agent.md";
        if (content == null) content = "";

        Map<String, String> meta = new HashMap<>();
        String body;

        Matcher m = FRONTMATTER.matcher(content);
        if (m.find()) {
            parseFrontmatter(m.group(1), meta);
            body = m.group(2);
        } else {
            body = content;
        }

        String id = filename.replaceAll("(?i)\\.md$", "");

        return new Agent(
            id,
            filename,                                 // exibido na UI
            meta.getOrDefault("description", ""),
            body == null ? "" : body.strip(),
            emptyToNull(meta.get("defaultPromptTemplate")),
            meta.getOrDefault("emoji", "✦"),
            "true".equalsIgnoreCase(meta.get("extractsTestCases"))
        );
    }

    /**
     * Parser simples de YAML — só `chave: valor` por linha. Aspas duplas e
     * simples são removidas. Linhas em branco e comentários (#) são ignorados.
     * Não suporta listas/objetos aninhados — propositalmente simples.
     */
    private static void parseFrontmatter(String fm, Map<String, String> out) {
        if (fm == null) return;
        for (String line : fm.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            int colon = trimmed.indexOf(':');
            if (colon <= 0) continue;

            String key = trimmed.substring(0, colon).trim();
            String value = trimmed.substring(colon + 1).trim();
            value = stripQuotes(value);
            if (!key.isEmpty()) out.put(key, value);
        }
    }

    private static String stripQuotes(String s) {
        if (s == null || s.length() < 2) return s;
        char first = s.charAt(0);
        char last = s.charAt(s.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}

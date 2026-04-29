package com.hefesto.testcases;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Parser que extrai test cases estruturados do markdown gerado pelo
 * agente "QA Specialist". Compatível com o template estrito definido no
 * AgentRegistry, mas tolerante a variações pequenas (ordem dos campos,
 * espaços extras, "—" vs "-", etc.).
 *
 * <p>Estratégia: split por headings "## TC-..." e parse de cada bloco
 * via regex específica por campo.</p>
 */
@Component
public class TestCaseExtractor {

    /** Casa cabeçalhos tipo "## TC-001 — [POSITIVO] Título" ou variantes. */
    private static final Pattern HEADER = Pattern.compile(
        "^##\\s*(TC[-\\s]?\\d+)\\s*[—\\-:]\\s*(?:\\[?([A-Za-zÁ-úÀ-ü]+)\\]?)?\\s*(.*?)\\s*$",
        Pattern.MULTILINE);

    private static final Pattern PRECONDITIONS = Pattern.compile(
        "(?im)\\*\\*\\s*Pr[ée]\\s*-?\\s*condi[cç][oõ]es?\\s*:?\\s*\\*\\*\\s*(.+?)(?=\\n\\s*\\n|\\n\\*\\*|\\Z)",
        Pattern.DOTALL);

    private static final Pattern STEPS = Pattern.compile(
        "(?im)\\*\\*\\s*Passos?\\s*:?\\s*\\*\\*\\s*\\n([\\s\\S]+?)(?=\\n\\s*\\*\\*|\\Z)");

    private static final Pattern EXPECTED = Pattern.compile(
        "(?im)\\*\\*\\s*Resultado\\s+esperado\\s*:?\\s*\\*\\*\\s*(.+?)(?=\\n\\s*\\n|\\n\\*\\*|\\Z)",
        Pattern.DOTALL);

    private static final Pattern PRIORITY = Pattern.compile(
        "(?im)\\*\\*\\s*Prioridade\\s*:?\\s*\\*\\*\\s*(P\\d|alta|m[ée]dia|baixa)",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern STEP_LINE = Pattern.compile(
        "^\\s*(?:\\d+\\.|\\-|\\*)\\s+(.+?)\\s*$");

    /**
     * Extrai todos os casos de teste de um conteúdo markdown.
     * Retorna lista vazia se não detectar nenhum.
     */
    public List<ParsedTestCase> extract(String markdown) {
        if (markdown == null || markdown.isBlank()) return List.of();

        // Encontra todas as posições dos headers de TC.
        List<int[]> headerSpans = new ArrayList<>();
        List<String[]> headerData = new ArrayList<>();
        Matcher m = HEADER.matcher(markdown);
        while (m.find()) {
            headerSpans.add(new int[]{m.start(), m.end()});
            headerData.add(new String[]{m.group(1), m.group(2), m.group(3)});
        }
        if (headerSpans.isEmpty()) return List.of();

        List<ParsedTestCase> result = new ArrayList<>();
        for (int i = 0; i < headerSpans.size(); i++) {
            int blockStart = headerSpans.get(i)[1];
            int blockEnd = (i + 1 < headerSpans.size())
                ? headerSpans.get(i + 1)[0]
                : markdown.length();
            String body = markdown.substring(blockStart, blockEnd);

            String code = normalizeCode(headerData.get(i)[0]);
            String category = normalizeCategory(headerData.get(i)[1]);
            String title = headerData.get(i)[2] == null ? "" : headerData.get(i)[2].strip();
            // Limpa "[CATEGORIA]" se ficou no título por engano.
            title = title.replaceAll("^\\[?[A-Za-zÁ-úÀ-ü]+\\]?\\s*[—\\-:]?\\s*", "").strip();

            String pre = matchOne(PRECONDITIONS, body);
            String steps = matchOne(STEPS, body);
            String expected = matchOne(EXPECTED, body);
            String priority = normalizePriority(matchOne(PRIORITY, body));

            List<String> stepList = splitSteps(steps);

            result.add(new ParsedTestCase(
                code,
                category,
                title.isEmpty() ? "Caso sem título" : title,
                pre,
                stepList,
                expected,
                priority,
                i  // position
            ));
        }
        return result;
    }

    private static String matchOne(Pattern p, String body) {
        Matcher m = p.matcher(body);
        if (m.find()) {
            String s = m.group(1);
            return s == null ? null : s.strip();
        }
        return null;
    }

    private static List<String> splitSteps(String stepsBlock) {
        if (stepsBlock == null || stepsBlock.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        for (String line : stepsBlock.split("\\r?\\n")) {
            Matcher m = STEP_LINE.matcher(line);
            if (m.matches()) {
                String step = m.group(1).strip();
                if (!step.isEmpty()) result.add(step);
            }
        }
        return result;
    }

    private static String normalizeCode(String raw) {
        if (raw == null) return null;
        // Remove espaços e normaliza pra TC-NNN
        String compact = raw.replaceAll("\\s+", "").toUpperCase();
        if (compact.startsWith("TC") && !compact.contains("-")) {
            return "TC-" + compact.substring(2);
        }
        return compact;
    }

    private static String normalizeCategory(String raw) {
        if (raw == null) return null;
        String up = raw.toUpperCase().strip();
        // Mapeia variações comuns.
        return switch (up) {
            case "POSITIVO", "POSITIVE", "POS", "HAPPY" -> "POSITIVO";
            case "NEGATIVO", "NEGATIVE", "NEG", "ERROR" -> "NEGATIVO";
            case "EDGE", "EDGECASE", "BORDA", "LIMITE" -> "EDGE";
            default -> up;
        };
    }

    private static String normalizePriority(String raw) {
        if (raw == null) return null;
        String up = raw.toUpperCase().strip();
        return switch (up) {
            case "P1", "ALTA" -> "P1";
            case "P2", "MEDIA", "MÉDIA" -> "P2";
            case "P3", "BAIXA" -> "P3";
            default -> up;
        };
    }

    /**
     * Resultado de uma extração — sem id ainda (será gerado pelo repositório).
     */
    public record ParsedTestCase(
        String code,
        String category,
        String title,
        String preconditions,
        List<String> steps,
        String expectedResult,
        String priority,
        int position
    ) {}
}

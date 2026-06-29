package com.hefesto.story;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Cobre o parser da matriz de cobertura (JSON em cerca, clamp, covered/uncovered, fallback). */
class CoverageServiceTest {

    @Test
    void parsesCriteriaWithCoverage() {
        String resp = "```json\n"
                + "{\"coveragePercent\":67,\"criteria\":["
                + "{\"criterion\":\"autentica com Google\",\"covered\":true,\"byTests\":[\"TC-001\",\"TC-003\"]},"
                + "{\"criterion\":\"vincula conta local\",\"covered\":true,\"byTests\":[\"TC-004\"]},"
                + "{\"criterion\":\"registra auditoria\",\"covered\":false,\"byTests\":[]}"
                + "]}\n```";
        CoverageReport r = CoverageService.parse(resp);
        assertThat(r.coveragePercent()).isEqualTo(67);
        assertThat(r.criteria()).hasSize(3);
        assertThat(r.criteria().get(0).covered()).isTrue();
        assertThat(r.criteria().get(0).byTests()).containsExactly("TC-001", "TC-003");
        assertThat(r.criteria().get(2).covered()).isFalse();
        assertThat(r.criteria().get(2).byTests()).isEmpty();
    }

    @Test
    void clampsPercent() {
        assertThat(CoverageService.parse("{\"coveragePercent\":120,\"criteria\":[{\"criterion\":\"x\"}]}")
                .coveragePercent()).isEqualTo(100);
    }

    @Test
    void fallsBackWhenNoJson() {
        CoverageReport r = CoverageService.parse("sem json");
        assertThat(r.coveragePercent()).isEqualTo(0);
        assertThat(r.criteria()).isEmpty();
    }
}

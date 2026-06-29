package com.hefesto.story;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Cobre o parser da revisão INVEST (JSON em cerca, score clamp, fallback). */
class StoryReviewServiceTest {

    @Test
    void parsesFencedJson() {
        String resp = "Segue a revisão:\n```json\n"
                + "{\"readinessScore\":72,\"verdict\":\"Quase pronta\","
                + "\"invest\":[\"I: ok\",\"T: falta criterio\"],"
                + "\"gaps\":[\"sem fluxo de erro\"],\"risks\":[\"dependencia externa\"],"
                + "\"missingCriteria\":[\"token expirado\"]}\n```";
        StoryReview r = StoryReviewService.parse(resp);
        assertThat(r.readinessScore()).isEqualTo(72);
        assertThat(r.verdict()).isEqualTo("Quase pronta");
        assertThat(r.gaps()).containsExactly("sem fluxo de erro");
        assertThat(r.risks()).containsExactly("dependencia externa");
        assertThat(r.missingCriteria()).containsExactly("token expirado");
        assertThat(r.invest()).hasSize(2);
    }

    @Test
    void clampsScoreToRange() {
        assertThat(StoryReviewService.parse("{\"readinessScore\":150}").readinessScore()).isEqualTo(100);
        assertThat(StoryReviewService.parse("{\"readinessScore\":-5}").readinessScore()).isEqualTo(0);
    }

    @Test
    void fallsBackWhenNoJson() {
        StoryReview r = StoryReviewService.parse("não estruturado");
        assertThat(r.readinessScore()).isEqualTo(0);
        assertThat(r.gaps()).isEmpty();
    }
}

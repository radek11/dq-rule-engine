package io.github.radek11.dq.rule;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.github.radek11.dq.result.Decision.INVALID;
import static io.github.radek11.dq.result.Decision.REVIEW;
import static io.github.radek11.dq.result.Decision.VALID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DecisionMappingTest {

    private final DecisionMapping mapping = DecisionMapping.of(Map.of("ok", VALID, "bad", INVALID), REVIEW);

    @Test
    void mappedValueGetsItsDecision() {
        assertThat(mapping.decide("ok")).isEqualTo(VALID);
        assertThat(mapping.decide("bad")).isEqualTo(INVALID);
    }

    @Test
    void unmappedValueGetsTheFallback() {
        assertThat(mapping.decide("unexpected")).isEqualTo(REVIEW);
    }

    @Test
    void valuesAreMatchedExactly() {
        assertThat(mapping.decide("OK")).isEqualTo(REVIEW);
        assertThat(mapping.decide("ok ")).isEqualTo(REVIEW);
    }

    @Test
    void laterChangesToTheSourceMapDoNotAffectTheMapping() {
        Map<String, io.github.radek11.dq.result.Decision> cases = new HashMap<>(Map.of("ok", VALID));
        DecisionMapping copied = DecisionMapping.of(cases, REVIEW);

        cases.put("ok", INVALID);

        assertThat(copied.decide("ok")).isEqualTo(VALID);
    }

    @Test
    void fallbackIsRequired() {
        assertThatNullPointerException().isThrownBy(() -> DecisionMapping.of(Map.of("ok", VALID), null));
    }
}

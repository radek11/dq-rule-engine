package io.github.radek11.dq.rule;

import java.util.Map;
import java.util.Objects;

/**
 * Declarative mapping from a rule's computed value to a {@link Decision}. The mapping is data:
 * rules never choose a decision themselves.
 *
 * <p>A fallback is mandatory, so every possible value has a decision and an unexpected value
 * never fails the run.
 */
public final class DecisionMapping {

    private final Map<String, Decision> cases;
    private final Decision fallback;

    private DecisionMapping(Map<String, Decision> cases, Decision fallback) {
        this.cases = Map.copyOf(cases);
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    /**
     * Creates a mapping.
     *
     * @param cases decision for each known value
     * @param fallback decision for any value not in {@code cases}
     * @return the mapping
     */
    public static DecisionMapping of(Map<String, Decision> cases, Decision fallback) {
        return new DecisionMapping(Objects.requireNonNull(cases, "cases"), fallback);
    }

    /**
     * Maps a computed value to a decision.
     *
     * @param value the value computed by the rule's logic, never {@code null}
     * @return the mapped decision, or the fallback for an unmapped value
     */
    public Decision decide(String value) {
        throw new UnsupportedOperationException("E2");
    }

    /** @return the decision for each known value */
    public Map<String, Decision> cases() {
        return cases;
    }

    /** @return the decision for any unmapped value */
    public Decision fallback() {
        return fallback;
    }
}

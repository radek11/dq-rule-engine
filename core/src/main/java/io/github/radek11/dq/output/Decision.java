package io.github.radek11.dq.output;

/**
 * The outcome of a rule for one record, derived from the rule's computed value through its
 * {@link io.github.radek11.dq.rule.DecisionMapping DecisionMapping}.
 */
public enum Decision {
    VALID,
    INVALID,
    REVIEW,
    NOT_APPLICABLE
}

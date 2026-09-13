package io.github.radek11.dq;

/**
 * The outcome of a rule for one record, derived from the rule's computed value through its
 * {@link DecisionMapping}.
 */
public enum Decision {
    VALID,
    INVALID,
    REVIEW,
    NOT_APPLICABLE
}

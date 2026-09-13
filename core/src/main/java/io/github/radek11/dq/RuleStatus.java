package io.github.radek11.dq;

/**
 * Lifecycle status of a rule. The engine runs {@link #RELEASED} rules only, whatever the
 * caller's filters select.
 */
public enum RuleStatus {
    DRAFT,
    RELEASED,
    DEPRECATED
}

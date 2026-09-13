package io.github.radek11.dq.rule;

/**
 * How serious a rule is. Every {@link io.github.radek11.dq.result.Result Result} carries the
 * severity of its rule, including results whose decision is {@link Decision#VALID}.
 */
public enum Severity {
    ERROR,
    WARNING,
    INFO
}

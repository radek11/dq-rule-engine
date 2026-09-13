package io.github.radek11.dq;

/**
 * How serious a rule is. Every {@link Result} carries the severity of its rule, including
 * results whose decision is {@link Decision#VALID}.
 */
public enum Severity {
    ERROR,
    WARNING,
    INFO
}

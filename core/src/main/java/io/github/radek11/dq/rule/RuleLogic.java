package io.github.radek11.dq.rule;

import io.github.radek11.dq.data.FieldReader;

/**
 * Computes a rule's value from a record, for example {@code "ok"} or {@code "blocked"}.
 * The value is turned into a decision by the rule's {@link DecisionMapping}.
 *
 * <p>Implementations must be stateless and thread-safe: one engine can run the same rule in
 * many runs at once.
 */
@FunctionalInterface
public interface RuleLogic {

    /**
     * Computes the value.
     *
     * <p>Faults become a {@link io.github.radek11.dq.result.Failure Failure} of kind
     * {@code RULE} for this rule on this record, and the run continues:
     * <ul>
     *   <li>any {@link RuntimeException} thrown here — including the
     *       {@link io.github.radek11.dq.data.MissingFieldException MissingFieldException} and
     *       {@link io.github.radek11.dq.data.FieldTypeException FieldTypeException} thrown by
     *       {@code fields} — is the failure's cause;</li>
     *   <li>a {@code null} return value gets an {@link IllegalStateException} as the cause.</li>
     * </ul>
     * A JVM {@link Error} is not isolated and ends the run.
     *
     * @param fields reader over the record; every read is recorded as provenance
     * @return the computed value
     */
    String compute(FieldReader fields);
}

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
     * @param fields reader over the record; every read is recorded as provenance
     * @return the computed value; {@code null} is reported as a failure of the rule
     */
    String compute(FieldReader fields);
}

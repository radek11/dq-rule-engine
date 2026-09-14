package io.github.radek11.dq.output;

import java.util.List;
import java.util.Objects;

/**
 * What one rule produced for one record, with the provenance that explains it: the rule, the
 * record, the fields the rule read and the value it computed from them.
 *
 * @param ruleId id of the rule
 * @param recordId id of the record
 * @param value value computed by the rule's logic
 * @param decision decision mapped from {@code value}
 * @param severity severity of the rule
 * @param fieldsRead fields read by the rule, in read order; a field read twice appears twice
 */
public record Result(
        String ruleId,
        String recordId,
        String value,
        Decision decision,
        Severity severity,
        List<FieldRead> fieldsRead) {

    public Result {
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(decision, "decision");
        Objects.requireNonNull(severity, "severity");
        fieldsRead = List.copyOf(Objects.requireNonNull(fieldsRead, "fieldsRead"));
    }
}

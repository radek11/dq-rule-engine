package io.github.radek11.dq;

import io.github.radek11.dq.rule.Rule;
import io.github.radek11.dq.rule.RuleFilters;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * How one run is performed. Immutable; use {@link #defaults()} and the {@code with} methods.
 *
 * @param batchSize number of records read per batch; memory use grows with this, not with input
 * @param ruleFilter selects the rules for the run, see {@link RuleFilters}; evaluated once per
 *     run, must be stateless
 * @param idField name of the field holding the record id; the value must be non-blank text
 * @param countryField name of the field holding the record country; the value must be text, or
 *     the field may be missing or {@code null}
 */
public record RunOptions(int batchSize, Predicate<Rule> ruleFilter, String idField, String countryField) {

    public RunOptions {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be at least 1, was " + batchSize);
        }
        Objects.requireNonNull(ruleFilter, "ruleFilter");
        requireFieldName(idField, "idField");
        requireFieldName(countryField, "countryField");
    }

    private static void requireFieldName(String name, String option) {
        Objects.requireNonNull(name, option);
        if (name.isBlank()) {
            throw new IllegalArgumentException(option + " must not be blank");
        }
    }

    /** @return batch size 1000, all rules, id in {@code id}, country in {@code country} */
    public static RunOptions defaults() {
        return new RunOptions(1000, RuleFilters.all(), "id", "country");
    }

    /** @param batchSize records per batch @return a copy with the batch size */
    public RunOptions withBatchSize(int batchSize) {
        return new RunOptions(batchSize, ruleFilter, idField, countryField);
    }

    /** @param ruleFilter rule selection @return a copy with the filter */
    public RunOptions withRuleFilter(Predicate<Rule> ruleFilter) {
        return new RunOptions(batchSize, ruleFilter, idField, countryField);
    }

    /** @param idField id field name @return a copy with the id field */
    public RunOptions withIdField(String idField) {
        return new RunOptions(batchSize, ruleFilter, idField, countryField);
    }

    /** @param countryField country field name @return a copy with the country field */
    public RunOptions withCountryField(String countryField) {
        return new RunOptions(batchSize, ruleFilter, idField, countryField);
    }
}

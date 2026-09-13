package io.github.radek11.dq;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * How one run is performed. Immutable; use {@link #defaults()} and the {@code with} methods.
 *
 * @param batchSize number of records read per batch; memory use grows with this, not with input
 * @param ruleFilter selects the rules for the run, see {@link RuleFilters}
 * @param idField name of the field holding the record id
 * @param countryField name of the field holding the record country
 */
public record RunOptions(int batchSize, Predicate<Rule> ruleFilter, String idField, String countryField) {

    public RunOptions {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be at least 1, was " + batchSize);
        }
        Objects.requireNonNull(ruleFilter, "ruleFilter");
        Objects.requireNonNull(idField, "idField");
        Objects.requireNonNull(countryField, "countryField");
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

package io.github.radek11.dq;

import io.github.radek11.dq.rule.Rule;
import io.github.radek11.dq.rule.RuleCatalog;

import java.util.List;
import java.util.function.Predicate;

/**
 * The rules of one run, fixed at its start: a snapshot of the catalog, checked for duplicate
 * ids, narrowed by the caller's filter and by the {@code RELEASED} invariant.
 */
final class RuleSelection {

    private final List<Rule> rules;

    private RuleSelection(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    /**
     * @param catalog source of rules, read once
     * @param filter the caller's filter
     * @return the selection
     * @throws IllegalArgumentException when two catalog rules share an id
     */
    static RuleSelection of(RuleCatalog catalog, Predicate<Rule> filter) {
        throw new UnsupportedOperationException("E2");
    }

    /** @return selected rules, in catalog order */
    List<Rule> rules() {
        return rules;
    }
}

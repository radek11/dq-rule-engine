package io.github.radek11.dq;

import io.github.radek11.dq.rule.Rule;
import io.github.radek11.dq.rule.RuleCatalog;
import io.github.radek11.dq.rule.RuleStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        List<Rule> snapshot = List.copyOf(catalog.rules());
        requireUniqueIds(snapshot);
        return new RuleSelection(snapshot.stream()
                .filter(filter)
                .filter(rule -> rule.status() == RuleStatus.RELEASED)
                .toList());
    }

    /** @return selected rules, in catalog order */
    List<Rule> rules() {
        return rules;
    }

    // Checked over the whole catalog, not the selection: a duplicate is a broken catalog
    // whichever of the two rules a filter would pick.
    private static void requireUniqueIds(List<Rule> rules) {
        Set<String> ids = new HashSet<>();
        for (Rule rule : rules) {
            if (!ids.add(rule.id())) {
                throw new IllegalArgumentException("Duplicate rule id in catalog: " + rule.id());
            }
        }
    }
}

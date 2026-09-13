package io.github.radek11.dq.rule;

import java.util.Collection;
import java.util.List;

/**
 * Source of rules — the seam for hosts that keep rules in code, files, a database or a remote
 * service.
 */
@FunctionalInterface
public interface RuleCatalog {

    /**
     * Returns the current rules. Called once at the start of each run; the run uses that
     * snapshot even if the catalog changes meanwhile. Rule ids must be unique — the engine
     * checks this before reading any record, for every catalog implementation.
     *
     * @return the rules
     */
    List<Rule> rules();

    /**
     * Creates an in-memory catalog holding a copy of the given rules.
     *
     * @param rules the rules
     * @return the catalog
     */
    static RuleCatalog of(Collection<Rule> rules) {
        List<Rule> copy = List.copyOf(rules);
        return () -> copy;
    }
}

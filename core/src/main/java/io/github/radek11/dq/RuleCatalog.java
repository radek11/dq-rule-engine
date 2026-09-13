package io.github.radek11.dq;

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
     * snapshot even if the catalog changes meanwhile.
     *
     * @return the rules, with unique ids
     */
    List<Rule> rules();

    /**
     * Creates an in-memory catalog.
     *
     * @param rules the rules
     * @return the catalog
     * @throws IllegalArgumentException when two rules share an id
     */
    static RuleCatalog of(Collection<Rule> rules) {
        throw new UnsupportedOperationException("E2");
    }
}

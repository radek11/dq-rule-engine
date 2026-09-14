package io.github.radek11.dq.rule;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Ready-made rule filters. Combine them with {@link Predicate#and}, {@link Predicate#or} and
 * {@link Predicate#negate}.
 *
 * <p>Filters narrow the run; they cannot widen it. Whatever they select, the engine runs
 * {@link RuleStatus#RELEASED} rules only, and only on records their {@link Scope} applies to.
 */
public final class RuleFilters {

    private RuleFilters() {
    }

    /** @return a filter that selects every rule */
    public static Predicate<Rule> all() {
        return rule -> true;
    }

    /**
     * Selects rules by status. Since the engine runs {@code RELEASED} rules only, a filter
     * without {@code RELEASED} selects nothing that runs.
     *
     * @param statuses accepted statuses, copied; none given selects no rule
     * @return a filter selecting rules with one of the statuses
     * @throws NullPointerException when the array or one of its elements is {@code null}
     */
    public static Predicate<Rule> status(RuleStatus... statuses) {
        Set<RuleStatus> accepted = Set.copyOf(Arrays.asList(statuses));
        return rule -> accepted.contains(rule.status());
    }

    /**
     * Selects rules by category. To require several categories at once, combine filters:
     * {@code category("a").and(category("b"))}.
     *
     * @param categories accepted categories, matched exactly, copied; none given selects no rule
     * @return a filter selecting rules that have at least one of the categories; a rule without
     *     categories is never selected
     * @throws NullPointerException when the array or one of its elements is {@code null}
     */
    public static Predicate<Rule> category(String... categories) {
        Set<String> accepted = Set.copyOf(Arrays.asList(categories));
        return rule -> !Collections.disjoint(rule.categories(), accepted);
    }

    /**
     * Selects rules by their declared scope, compared by equality. It does not look at records:
     * {@code scope(Scope.country("DE"))} leaves out {@link Scope#WORLD} rules, so to run
     * everything that applies to German records pass both {@code WORLD} and {@code DE}.
     *
     * @param scopes accepted scopes, copied; none given selects no rule
     * @return a filter selecting rules whose scope is one of the given scopes
     * @throws NullPointerException when the array or one of its elements is {@code null}
     */
    public static Predicate<Rule> scope(Scope... scopes) {
        Set<Scope> accepted = Set.copyOf(Arrays.asList(scopes));
        return rule -> accepted.contains(rule.scope());
    }
}

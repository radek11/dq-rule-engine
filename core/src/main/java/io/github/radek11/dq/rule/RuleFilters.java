package io.github.radek11.dq.rule;

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
     * @param statuses accepted statuses; none given selects no rule
     * @return a filter selecting rules with one of the statuses
     */
    public static Predicate<Rule> status(RuleStatus... statuses) {
        throw new UnsupportedOperationException("E4");
    }

    /**
     * @param categories accepted categories, matched exactly; none given selects no rule
     * @return a filter selecting rules that have at least one of the categories; a rule without
     *     categories is never selected
     */
    public static Predicate<Rule> category(String... categories) {
        throw new UnsupportedOperationException("E4");
    }

    /**
     * @param scopes accepted scopes; none given selects no rule
     * @return a filter selecting rules whose scope is one of the given scopes
     */
    public static Predicate<Rule> scope(Scope... scopes) {
        throw new UnsupportedOperationException("E4");
    }
}

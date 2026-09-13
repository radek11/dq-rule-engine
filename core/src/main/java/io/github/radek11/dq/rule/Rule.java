package io.github.radek11.dq.rule;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/**
 * A named data-quality check. Immutable.
 *
 * <p>Equality compares all components, including {@code logic} and {@code mapping}, which use
 * identity equality — two rules built separately from the same lambda are not equal. Use
 * {@link #id()} to identify a rule.
 *
 * @param id stable identifier, unique within a catalog
 * @param label human-readable name
 * @param status lifecycle status
 * @param severity severity carried by every result of this rule
 * @param scope where the rule applies
 * @param categories zero or more categories
 * @param logic computes the value from the record
 * @param mapping turns the value into a decision
 */
public record Rule(
        String id,
        String label,
        RuleStatus status,
        Severity severity,
        Scope scope,
        Set<String> categories,
        RuleLogic logic,
        DecisionMapping mapping) {

    public Rule {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("rule id must not be blank");
        }
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(scope, "scope");
        categories = Set.copyOf(Objects.requireNonNull(categories, "categories"));
        Objects.requireNonNull(logic, "logic");
        Objects.requireNonNull(mapping, "mapping");
    }

    /**
     * Starts building a rule with scope {@link Scope#WORLD} and no categories.
     *
     * @param id stable identifier
     * @return the builder
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /** Builder for {@link Rule}. Not thread-safe. */
    public static final class Builder {
        private final String id;
        private String label;
        private RuleStatus status;
        private Severity severity;
        private Scope scope = Scope.WORLD;
        private Set<String> categories = Set.of();
        private RuleLogic logic;
        private DecisionMapping mapping;

        private Builder(String id) {
            this.id = id;
        }

        /** @param label human-readable name @return this builder */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /** @param status lifecycle status @return this builder */
        public Builder status(RuleStatus status) {
            this.status = status;
            return this;
        }

        /** @param severity severity of results @return this builder */
        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }

        /** @param scope where the rule applies @return this builder */
        public Builder scope(Scope scope) {
            this.scope = scope;
            return this;
        }

        /**
         * Sets the categories. Duplicates are ignored; categories are matched exactly,
         * including case.
         *
         * @param categories categories of the rule
         * @return this builder
         */
        public Builder categories(String... categories) {
            this.categories = Set.copyOf(Arrays.asList(categories));
            return this;
        }

        /** @param logic computes the value @return this builder */
        public Builder logic(RuleLogic logic) {
            this.logic = logic;
            return this;
        }

        /** @param mapping turns the value into a decision @return this builder */
        public Builder mapping(DecisionMapping mapping) {
            this.mapping = mapping;
            return this;
        }

        /**
         * @return the rule
         * @throws NullPointerException when a required property is not set
         */
        public Rule build() {
            return new Rule(id, label, status, severity, scope, categories, logic, mapping);
        }
    }
}

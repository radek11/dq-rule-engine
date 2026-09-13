package io.github.radek11.dq.rule;

import io.github.radek11.dq.data.DataRecord;
import io.github.radek11.dq.result.Result;
import io.github.radek11.dq.result.Severity;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

/**
 * A named data-quality check that evaluates itself against a record. Immutable and safe to
 * share between threads, provided its {@link RuleLogic} is stateless.
 *
 * <p>The logic and the decision mapping are private: the only way to run a rule is
 * {@link #evaluate}, which always applies the mapping and the rule's contract. Rules are
 * identified by {@link #id()}; equality is identity.
 */
public final class Rule {

    private final String id;
    private final String label;
    private final RuleStatus status;
    private final Severity severity;
    private final Scope scope;
    private final Set<String> categories;
    private final RuleLogic logic;
    private final DecisionMapping mapping;

    private Rule(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("rule id must not be blank");
        }
        this.label = Objects.requireNonNull(builder.label, "label");
        this.status = Objects.requireNonNull(builder.status, "status");
        this.severity = Objects.requireNonNull(builder.severity, "severity");
        this.scope = Objects.requireNonNull(builder.scope, "scope");
        this.categories = Set.copyOf(builder.categories);
        this.logic = Objects.requireNonNull(builder.logic, "logic");
        this.mapping = Objects.requireNonNull(builder.mapping, "mapping");
    }

    /**
     * Starts building a rule with scope {@link Scope#WORLD} and no categories.
     *
     * @param id stable identifier, unique within a catalog
     * @return the builder
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Evaluates this rule on one record: runs the logic through a recording field reader, maps
     * the computed value to a decision and returns the result with its provenance.
     *
     * <p>Does not isolate faults — that is the engine's job. Any {@link RuntimeException} from
     * the logic propagates; a {@code null} value from the logic is reported as
     * {@link IllegalStateException}.
     *
     * @param record the record
     * @param recordId the record's id, carried into the result
     * @return the result
     */
    public Result evaluate(DataRecord record, String recordId) {
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(recordId, "recordId");
        RecordingFieldReader fields = new RecordingFieldReader(record);
        String value = logic.compute(fields);
        if (value == null) {
            throw new IllegalStateException("Rule " + id + " computed no value");
        }
        return new Result(id, recordId, value, mapping.decide(value), severity, fields.reads());
    }

    /**
     * @param recordCountry the record's country, or {@code null} when it has none
     * @return whether this rule applies to a record of that country, see {@link Scope}
     */
    public boolean appliesTo(String recordCountry) {
        throw new UnsupportedOperationException("E4");
    }

    /** @return stable identifier */
    public String id() {
        return id;
    }

    /** @return human-readable name */
    public String label() {
        return label;
    }

    /** @return lifecycle status */
    public RuleStatus status() {
        return status;
    }

    /** @return severity carried by every result of this rule */
    public Severity severity() {
        return severity;
    }

    /** @return where the rule applies */
    public Scope scope() {
        return scope;
    }

    /** @return categories, possibly empty */
    public Set<String> categories() {
        return categories;
    }

    @Override
    public String toString() {
        return "Rule[" + id + ", " + status + ", " + severity + ", " + scope + ", " + categories + "]";
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

        /**
         * @param label human-readable name
         * @return this builder
         */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * @param status lifecycle status
         * @return this builder
         */
        public Builder status(RuleStatus status) {
            this.status = status;
            return this;
        }

        /**
         * @param severity severity carried by every result
         * @return this builder
         */
        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }

        /**
         * @param scope where the rule applies
         * @return this builder
         */
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

        /**
         * @param logic computes the value from the record
         * @return this builder
         */
        public Builder logic(RuleLogic logic) {
            this.logic = logic;
            return this;
        }

        /**
         * @param mapping turns the value into a decision
         * @return this builder
         */
        public Builder mapping(DecisionMapping mapping) {
            this.mapping = mapping;
            return this;
        }

        /**
         * @return the rule
         * @throws NullPointerException when a required property is not set
         * @throws IllegalArgumentException when the id is blank
         */
        public Rule build() {
            return new Rule(this);
        }
    }
}

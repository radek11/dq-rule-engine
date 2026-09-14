package io.github.radek11.dq.rule;

import java.util.Locale;
import java.util.Objects;

/**
 * Where a rule applies: everywhere ({@link #WORLD}), or to records of a single country.
 * Immutable value object.
 *
 * <p>Applicability is always enforced by the engine: a rule whose scope does not apply to a
 * record produces no {@link io.github.radek11.dq.output.Result Result} for it.
 */
public final class Scope {

    /** Scope of rules that apply to every record, including records without a country. */
    public static final Scope WORLD = new Scope(null);

    /** Upper-case ISO 3166-1 alpha-2 code, or {@code null} for {@link #WORLD}. */
    private final String country;

    private Scope(String country) {
        this.country = country;
    }

    /**
     * Scope of rules that apply to one country.
     *
     * @param code ISO 3166-1 alpha-2 code, in any case; it is not checked against the ISO list
     * @return the scope
     */
    public static Scope country(String code) {
        Objects.requireNonNull(code, "code");
        if (code.isBlank()) {
            throw new IllegalArgumentException("country code must not be blank");
        }
        return new Scope(normalize(code));
    }

    /**
     * Whether a rule with this scope applies to a record. The record's country is compared
     * after the same normalization as the scope's code: stripped, upper-case.
     *
     * @param recordCountry the record's country, or {@code null} when it has none; a record
     *     without a country gets {@link #WORLD} rules only
     * @return {@code true} when the rule should run on the record
     */
    boolean appliesTo(String recordCountry) {
        throw new UnsupportedOperationException("E4");
    }

    static String normalize(String code) {
        return code.strip().toUpperCase(Locale.ROOT);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Scope that && Objects.equals(country, that.country);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(country);
    }

    @Override
    public String toString() {
        return country == null ? "WORLD" : country;
    }
}

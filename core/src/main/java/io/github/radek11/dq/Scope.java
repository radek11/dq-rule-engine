package io.github.radek11.dq;

import java.util.Locale;
import java.util.Objects;

/**
 * Where a rule applies: everywhere, or to records of a single country.
 *
 * <p>Applicability is always enforced by the engine: a rule whose scope does not apply to a
 * record produces no {@link Result} for it.
 */
public sealed interface Scope {

    /** Scope of rules that apply to every record, including records without a country. */
    Scope WORLD = new World();

    /**
     * Scope of rules that apply to one country.
     *
     * @param code ISO 3166-1 alpha-2 code; compared case-insensitively
     * @return the scope
     */
    static Scope country(String code) {
        return new Country(code);
    }

    /**
     * Whether a rule with this scope applies to a record.
     *
     * @param recordCountry the record's country, or {@code null} when the record has none
     * @return {@code true} when the rule should run on the record
     */
    boolean appliesTo(String recordCountry);

    /** Applies to every record. */
    record World() implements Scope {
        @Override
        public boolean appliesTo(String recordCountry) {
            throw new UnsupportedOperationException("E4");
        }
    }

    /**
     * Applies to records of one country.
     *
     * @param code upper-case ISO 3166-1 alpha-2 code
     */
    record Country(String code) implements Scope {
        public Country {
            Objects.requireNonNull(code, "code");
            if (code.isBlank()) {
                throw new IllegalArgumentException("country code must not be blank");
            }
            code = code.strip().toUpperCase(Locale.ROOT);
        }

        @Override
        public boolean appliesTo(String recordCountry) {
            throw new UnsupportedOperationException("E4");
        }
    }
}

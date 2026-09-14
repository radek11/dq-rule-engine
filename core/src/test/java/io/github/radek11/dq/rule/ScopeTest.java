package io.github.radek11.dq.rule;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ScopeTest {

    private final Scope germany = Scope.country("DE");

    @Test
    void aWorldRuleAppliesToEveryRecordIncludingOneWithoutACountry() {
        assertThat(Scope.WORLD.appliesTo("DE")).isTrue();
        assertThat(Scope.WORLD.appliesTo(null)).isTrue();
        assertThat(Scope.WORLD.appliesTo("")).isTrue();
    }

    @Test
    void aCountryRuleAppliesToRecordsOfThatCountryOnly() {
        assertThat(germany.appliesTo("DE")).isTrue();
        assertThat(germany.appliesTo("FR")).isFalse();
    }

    @Test
    void aCountryRuleDoesNotApplyToARecordWithoutACountry() {
        assertThat(germany.appliesTo(null)).isFalse();
        assertThat(germany.appliesTo("")).isFalse();
        assertThat(germany.appliesTo("  ")).isFalse();
    }

    @Test
    void theRecordCountryIsComparedAfterStrippingAndUpperCasing() {
        assertThat(germany.appliesTo("de")).isTrue();
        assertThat(germany.appliesTo(" De ")).isTrue();
    }

    @Test
    void theScopeCodeIsNormalizedTheSameWay() {
        assertThat(Scope.country(" de ")).isEqualTo(germany).hasSameHashCodeAs(germany);
        assertThat(Scope.country(" de ")).hasToString("DE");
    }

    @Test
    void scopesOfDifferentCountriesAndWorldAreNotEqual() {
        assertThat(germany).isNotEqualTo(Scope.country("FR")).isNotEqualTo(Scope.WORLD);
        assertThat(Scope.WORLD).isNotEqualTo(germany);
    }

    @Test
    void aCountryCodeIsRequiredAndMustNotBeBlank() {
        assertThatNullPointerException().isThrownBy(() -> Scope.country(null));
        assertThatIllegalArgumentException().isThrownBy(() -> Scope.country(""));
        assertThatIllegalArgumentException().isThrownBy(() -> Scope.country("  "));
    }
}

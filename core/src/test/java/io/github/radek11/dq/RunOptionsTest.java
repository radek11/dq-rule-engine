package io.github.radek11.dq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.github.radek11.dq.Fixture.RULES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class RunOptionsTest {

    @Test
    void theDefaultsAreBatchSize1000AllRulesAndFieldsIdAndCountry() {
        RunOptions defaults = RunOptions.defaults();

        assertThat(defaults.batchSize()).isEqualTo(1000);
        assertThat(RULES).allMatch(defaults.ruleFilter());
        assertThat(defaults.idField()).isEqualTo("id");
        assertThat(defaults.countryField()).isEqualTo("country");
    }

    // K8 — the batch size is set by the caller, at least 1

    @Test
    void aBatchSizeOfOneIsAccepted() {
        assertThat(RunOptions.defaults().withBatchSize(1).batchSize()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void aBatchSizeBelowOneIsRejected(int batchSize) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> RunOptions.defaults().withBatchSize(batchSize))
                .withMessageContaining("batchSize");
    }

    // Rule filter and field names

    @Test
    void aNullRuleFilterIsRejected() {
        assertThatNullPointerException()
                .isThrownBy(() -> RunOptions.defaults().withRuleFilter(null))
                .withMessage("ruleFilter");
    }

    @Test
    void aNullIdFieldIsRejected() {
        assertThatNullPointerException()
                .isThrownBy(() -> RunOptions.defaults().withIdField(null))
                .withMessage("idField");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void aBlankIdFieldIsRejected(String idField) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> RunOptions.defaults().withIdField(idField))
                .withMessageContaining("idField");
    }

    @Test
    void aNullCountryFieldIsRejected() {
        assertThatNullPointerException()
                .isThrownBy(() -> RunOptions.defaults().withCountryField(null))
                .withMessage("countryField");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void aBlankCountryFieldIsRejected(String countryField) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> RunOptions.defaults().withCountryField(countryField))
                .withMessageContaining("countryField");
    }
}

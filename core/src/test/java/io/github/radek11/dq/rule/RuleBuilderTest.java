package io.github.radek11.dq.rule;

import io.github.radek11.dq.output.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static io.github.radek11.dq.output.Decision.REVIEW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class RuleBuilderTest {

    @Test
    void aRuleBuiltWithoutScopeOrCategoriesAppliesToTheWorldAndHasNoCategories() {
        Rule rule = complete("check").build();

        assertThat(rule.scope()).isEqualTo(Scope.WORLD);
        assertThat(rule.categories()).isEmpty();
    }

    @Test
    void theGivenScopeAndCategoriesAreKept() {
        Rule rule = complete("check").scope(Scope.country("DE")).categories("format", "compliance").build();

        assertThat(rule.scope()).isEqualTo(Scope.country("DE"));
        assertThat(rule.categories()).containsExactlyInAnyOrder("format", "compliance");
    }

    @Test
    void duplicateCategoriesAreIgnoredAndCaseIsKept() {
        Rule rule = complete("check").categories("format", "format", "Format").build();

        assertThat(rule.categories()).containsExactlyInAnyOrder("format", "Format");
    }

    @ParameterizedTest
    @ValueSource(strings = {"label", "status", "severity", "scope", "logic", "mapping"})
    void aRequiredPropertyLeftUnsetIsReportedByName(String property) {
        Rule.Builder builder = complete("check");
        switch (property) {
            case "label" -> builder.label(null);
            case "status" -> builder.status(null);
            case "severity" -> builder.severity(null);
            case "scope" -> builder.scope(null);
            case "logic" -> builder.logic(null);
            case "mapping" -> builder.mapping(null);
            default -> throw new IllegalArgumentException(property);
        }

        assertThatNullPointerException().isThrownBy(builder::build).withMessage(property);
    }

    @Test
    void theIdIsRequiredAndMustNotBeBlank() {
        assertThatNullPointerException().isThrownBy(() -> complete(null).build()).withMessage("id");
        assertThatIllegalArgumentException().isThrownBy(() -> complete("").build());
        assertThatIllegalArgumentException().isThrownBy(() -> complete("  ").build());
    }

    private static Rule.Builder complete(String id) {
        return Rule.builder(id).label("label").status(RuleStatus.RELEASED).severity(Severity.INFO)
                .logic(fields -> "ok")
                .mapping(DecisionMapping.of(Map.of(), REVIEW));
    }
}

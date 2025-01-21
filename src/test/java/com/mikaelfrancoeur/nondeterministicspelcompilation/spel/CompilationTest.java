package com.mikaelfrancoeur.nondeterministicspelcompilation.spel;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelCompiler;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class CompilationTest implements WithAssertions {

    @Test
    void compilableExpression() {
        Expression expression = new SpelExpressionParser().parseExpression("left && right");
        SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
                .withRootObject(new ExpressionRoot(true, true))
                .build();

        assertThat(SpelCompiler.compile(expression)).isFalse();

        Object result = expression.getValue(context);
        assertThat(result).isEqualTo(true);

        assertThat(SpelCompiler.compile(expression)).isTrue();
    }

    @Test
    void unCompilableExpression() {
        Expression expression = new SpelExpressionParser().parseExpression("left && right");
        SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
                .withRootObject(new ExpressionRoot(false, true))
                .build();

        assertThat(SpelCompiler.compile(expression))
                .isFalse();

        assertThat(expression.getValue(context))
                .isEqualTo(false);

        assertThat(SpelCompiler.compile(expression))
                .isFalse();
    }

    @Test
    void unCompilableExpressionWithCustomParser() {
        Expression expression = new CustomSpelExpressionParser().parseExpression("left && right");
        SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
                .withRootObject(new ExpressionRoot(false, true))
                .build();

        assertThat(SpelCompiler.compile(expression))
                .isFalse();

        assertThat(expression.getValue(context))
                .isEqualTo(false);

        assertThat(SpelCompiler.compile(expression))
                .isTrue();
    }

    public record ExpressionRoot(Boolean left, Boolean right) {
    }

    @Test
    void nonDeterminisicCompilability() {
        Expression expression = new SpelExpressionParser()
                .parseExpression("T(java.util.concurrent.ThreadLocalRandom).current().nextBoolean() && right");

        EvaluationContext context = new StandardEvaluationContext(new ExpressionRoot(null, true));

        assertThat(SpelCompiler.compile(expression)).isFalse();
        assertThat(expression.getValue(context)).isEqualTo(false);
        assertThat(SpelCompiler.compile(expression)).isFalse();
    }
}

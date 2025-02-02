package com.mikaelfrancoeur.nondeterministicspelcompilation.spel;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Disabled;
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
    @Disabled("this demo is easier to understand if the last assertion fails")
    void unCompilableExpression() {
        // declare and parse an expression
        Expression expression = new SpelExpressionParser().parseExpression("left and right");

        // set the values of "left" and "right" on the evaluation context
        SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
                .withRootObject(new ExpressionRoot(false, true))
                .build();

        // SpelCompiler.compile() tries to compile the expression,
        // returning true if it succeeded, and false otherwise.
        // If true, further evaluations of the `SpelExpression` will
        // run in compiled mode.
        // TODO check if they will revert to interpreted on exceptions if compiler is off or mixed
        assertThat(SpelCompiler.compile(expression))
                .isFalse();

        // evaluate the expression
        assertThat(expression.getValue(context))
                .isEqualTo(false);

        // check that the expression is compilable;
        // this fails because the right operand has not been evaluated yet
        assertThat(SpelCompiler.compile(expression))
                .isTrue();
    }

    @Test
    void unCompilableAndExpressionWithCustomParser() {
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

    @Test
    void uncompilableOrExpressionWithCustomParser() {
        Expression expression = new CustomSpelExpressionParser().parseExpression("left or right");
        SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
                .withRootObject(new ExpressionRoot(true, false))
                .build();

        assertThat(SpelCompiler.compile(expression))
                .isFalse();

        assertThat(expression.getValue(context))
                .isEqualTo(true);

        assertThat(SpelCompiler.compile(expression))
                .isTrue();
    }

    @Test
    void uncompilableTernaryExpressionWithCustomParser() {
        Expression expression = new CustomSpelExpressionParser().parseExpression("left ? right : #third");
        SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
                .withRootObject(new ExpressionRoot(true, true))
                .build();
        context.setVariable("third", false);

        assertThat(SpelCompiler.compile(expression))
                .isFalse();

        assertThat(expression.getValue(context))
                .isEqualTo(true);

        assertThat(SpelCompiler.compile(expression))
                .isTrue();
    }

    public record ExpressionRoot(Boolean left, Boolean right) {
    }

    @Test
    @Disabled("the purpose of this test is to demonstrate that the compilability of the expression is not deterministic")
    void nonDeterminisicCompilability() {
        Expression expression = new SpelExpressionParser()
                .parseExpression("T(java.util.concurrent.ThreadLocalRandom).current().nextBoolean() && right");

        EvaluationContext context = new StandardEvaluationContext(new ExpressionRoot(null, true));

        assertThat(SpelCompiler.compile(expression)).isFalse();
        assertThat(expression.getValue(context)).isEqualTo(false);
        assertThat(SpelCompiler.compile(expression)).isFalse();
    }
}

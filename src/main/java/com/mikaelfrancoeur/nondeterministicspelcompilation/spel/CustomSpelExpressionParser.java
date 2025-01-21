package com.mikaelfrancoeur.nondeterministicspelcompilation.spel;

import java.util.Arrays;
import java.util.Objects;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.expression.spel.ast.OpOr;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.BooleanTypedValue;
import org.springframework.lang.Nullable;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomSpelExpressionParser extends SpelExpressionParser {

    private final SpelParserConfiguration configuration = new SpelParserConfiguration();
    private final SpelExpressionParser delegate = new SpelExpressionParser(configuration);

    @Override
    public Expression parseExpression(String expressionString, @Nullable ParserContext context) throws ParseException {
        SpelExpression expression = (SpelExpression) delegate.parseExpression(expressionString, context);

        return new SpelExpression(expressionString, transformAst((SpelNodeImpl) expression.getAST()), configuration);
    }

    @Override
    public Expression parseExpression(String expressionString) throws ParseException {
        return parseExpression(expressionString, null);
    }

    public SpelNodeImpl transformAst(SpelNodeImpl root) {
        return switch (root) {
            case OpAnd opAnd -> new NonShortCircuitingFirstEvaluationAnd(
                    opAnd.getStartPosition(),
                    opAnd.getEndPosition(),
                    transformChildren(getChildren(opAnd)));
            case OpOr opOr -> new NonShortCircuitingFirstEvaluationAnd(
                    opOr.getStartPosition(),
                    opOr.getEndPosition(),
                    transformChildren(getChildren(opOr)));
            default -> root;
        };
    }

    private SpelNodeImpl[] transformChildren(SpelNodeImpl... children) {
        return Arrays.stream(children)
                .map(this::transformAst)
                .toArray(SpelNodeImpl[]::new);
    }

    private SpelNodeImpl[] getChildren(SpelNodeImpl root) {
        SpelNodeImpl[] children = new SpelNodeImpl[root.getChildCount()];
        for (int i = 0; i < root.getChildCount(); i++) {
            children[i] = (SpelNodeImpl) root.getChild(i);
        }
        return children;
    }

    private static class NonShortCircuitingFirstEvaluationAnd extends OpAnd {

        private volatile boolean initialized;

        public NonShortCircuitingFirstEvaluationAnd(int startPos, int endPos, SpelNodeImpl... operands) {
            super(startPos, endPos, operands);
        }

        @Override
        public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        initialized = true;

                        Boolean left = (Boolean) getLeftOperand().getValue(state);
                        Boolean right = (Boolean) getRightOperand().getValue(state);

                        Objects.requireNonNull(left);
                        Objects.requireNonNull(right);

                        return BooleanTypedValue.forValue(left && right);
                    }
                }
            }

            return super.getValueInternal(state);
        }
    }
}

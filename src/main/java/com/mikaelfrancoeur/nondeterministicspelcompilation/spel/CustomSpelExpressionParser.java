package com.mikaelfrancoeur.nondeterministicspelcompilation.spel;

import java.util.Arrays;
import java.util.Objects;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.expression.spel.ast.OpOr;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.expression.spel.ast.Ternary;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.BooleanTypedValue;
import org.springframework.lang.Nullable;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomSpelExpressionParser extends SpelExpressionParser {

    private final SpelParserConfiguration configuration = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null);
    private final SpelExpressionParser delegate = new SpelExpressionParser(configuration);

    @Override
    public Expression parseExpression(String expressionString, @Nullable ParserContext context) throws ParseException {
        SpelExpression expression = (SpelExpression) delegate.parseExpression(expressionString, context);

        SpelNode ast = expression.getAST();

        return new SpelExpression(expressionString, transformAst((SpelNodeImpl) ast), configuration);
    }

    // ...

    public SpelNodeImpl transformAst(SpelNodeImpl root) {
        return switch (root) {
            case OpAnd opAnd -> new NonShortCircuitingWhenInterpretedAnd(
                    opAnd.getStartPosition(),
                    opAnd.getEndPosition(),
                    // recursive case
                    transformChildren(getChildren(opAnd)));
            case OpOr opOr -> new NonShortCircuitingWhenInterpretedOr(
                    opOr.getStartPosition(),
                    opOr.getEndPosition(),
                    // recursive case
                    transformChildren(getChildren(opOr)));
            case Ternary ternary -> new NonShortCircuitingWhenInterpretedTernary(
                    ternary.getStartPosition(),
                    ternary.getEndPosition(),
                    // recursive case
                    transformChildren(getChildren(ternary)));
            // base case
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

    private static class NonShortCircuitingWhenInterpretedAnd extends OpAnd {

        public NonShortCircuitingWhenInterpretedAnd(int startPos, int endPos, SpelNodeImpl... operands) {
            super(startPos, endPos, operands);
        }

        @Override
        //TODO add error reporting from superclass
        public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
            Boolean left = (Boolean) getLeftOperand().getValue(state);
            Boolean right = (Boolean) getRightOperand().getValue(state);

            Objects.requireNonNull(left);
            Objects.requireNonNull(right);

            return BooleanTypedValue.forValue(left && right);
        }
    }

    private static class NonShortCircuitingWhenInterpretedOr extends OpOr {

        public NonShortCircuitingWhenInterpretedOr(int startPos, int endPos, SpelNodeImpl... operands) {
            super(startPos, endPos, operands);
        }

        @Override
        //TODO add error reporting from superclass
        public BooleanTypedValue getValueInternal(ExpressionState state) throws EvaluationException {
            Boolean left = (Boolean) getLeftOperand().getValue(state);
            Boolean right = (Boolean) getRightOperand().getValue(state);

            Objects.requireNonNull(left);
            Objects.requireNonNull(right);

            return BooleanTypedValue.forValue(left || right);
        }
    }

    private static class NonShortCircuitingWhenInterpretedTernary extends Ternary {

        public NonShortCircuitingWhenInterpretedTernary(int startPos, int endPos, SpelNodeImpl... args) {
            super(startPos, endPos, args);
        }

        @Override
        //TODO add error reporting from superclass
        public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
            Boolean one = (Boolean) getChild(0).getValue(state);
            Boolean two = (Boolean) getChild(1).getValue(state);
            Boolean three = (Boolean) getChild(2).getValue(state);

            Objects.requireNonNull(one);
            Objects.requireNonNull(two);
            Objects.requireNonNull(three);

            return BooleanTypedValue.forValue(one ? two : three);
        }
    }
}

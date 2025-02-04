package chill.script.expressions;

import chill.script.parser.ChillScriptParser;
import chill.script.runtime.ChillScriptRuntime;
import chill.script.tokenizer.Token;
import chill.script.tokenizer.TokenType;

import java.math.BigDecimal;

public class UnaryExpression extends Expression {

    public static final BigDecimal NEGATIVE_ONE = new BigDecimal("-1");
    private final Token operator;
    private final Expression rightHandSide;

    public UnaryExpression(Token operator, Expression rightHandSide) {
        this.rightHandSide = addChild(rightHandSide);
        this.operator = operator;
    }


    public Expression getRightHandSide() {
        return rightHandSide;
    }

    @Override
    public String toString() {
        return super.toString() + "[" + operator.getStringValue() + "]";
    }

    @Override
    public Object evaluate(ChillScriptRuntime runtime) {
        Object rhsValue = rightHandSide.evaluate(runtime);
        if (operator.getStringValue().equals("-")) {
            if (rhsValue instanceof BigDecimal bigDecimal) {
                return bigDecimal.multiply(NEGATIVE_ONE);
            }
        }
        throw new UnsupportedOperationException(operator.getStringValue() + " not implemented for this type yet");
    }

    public static Expression parse(ChillScriptParser parser) {
        parser.matchAndConsume("the"); // optional 'the'
        if (parser.match(TokenType.MINUS)) {
            Token operator = parser.consumeToken();
            final Expression rightHandSide = parser.parse("unaryExpression");
            var unaryExpr = new UnaryExpression(operator, rightHandSide);
            unaryExpr.setEnd(rightHandSide.getEnd());
            return unaryExpr;
        } else {
            return parser.parse("indirectExpression");
        }
    }
}

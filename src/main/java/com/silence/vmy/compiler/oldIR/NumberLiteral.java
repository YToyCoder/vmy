package com.silence.vmy.compiler.oldIR;

public class NumberLiteral extends LiteralNode {
    final Number val;

    public NumberLiteral(Number _number) {
        super(0);
        val = _number;
    }

    @Override public Object val() { return val; }
}

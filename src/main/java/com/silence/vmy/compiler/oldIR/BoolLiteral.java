package com.silence.vmy.compiler.oldIR;

public class BoolLiteral extends LiteralNode {
    final Boolean value;

    public BoolLiteral(Boolean _value) {
        super(0);
        value = _value;
    }

    @Override public Object val() { return value; }
}

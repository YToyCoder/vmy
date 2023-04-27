package com.silence.vmy.compiler.oldIR;

public class StringLiteral extends LiteralNode {
    private final String value;

    public StringLiteral(String value) {
        super(0);
        this.value = value;
    }

    @Override public Object val() { return value; }
}

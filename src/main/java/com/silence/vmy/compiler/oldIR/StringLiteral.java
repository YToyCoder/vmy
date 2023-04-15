package com.silence.vmy.compiler.oldIR;

import com.silence.vmy.compiler.AST;

public class StringLiteral extends LiteralNode {
    private final String value;

    public StringLiteral(String value) {
        super(AST.LiteralKind.String.ordinal());
        this.value = value;
    }

    @Override public Object val() { return value; }
}

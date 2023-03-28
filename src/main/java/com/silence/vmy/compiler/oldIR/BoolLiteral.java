package com.silence.vmy.compiler.oldIR;

import com.silence.vmy.compiler.AST;

public class BoolLiteral extends LiteralNode {
    final Boolean value;

    public BoolLiteral(Boolean _value) {
        super(AST.LiteralKind.Bool.ordinal());
        value = _value;
    }

    @Override
    public Object val() {
        return value;
    }
}

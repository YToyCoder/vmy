package com.silence.vmy.compiler.oldIR;

import com.silence.vmy.compiler.AST;

public class NumberLiteral extends LiteralNode {
    final Number val;

    public NumberLiteral(Number _number) {
        super(_number instanceof Integer ? AST.LiteralKind.Int.ordinal() : AST.LiteralKind.Double.ordinal());
        val = _number;
    }

    @Override public Object val() { return val; }
}

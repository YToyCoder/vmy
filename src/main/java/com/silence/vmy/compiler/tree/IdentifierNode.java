package com.silence.vmy.compiler.tree;

import com.silence.vmy.compiler.visitor.NodeVisitor;

// node for Identifier , like variable-name/function-name ...
public class IdentifierNode implements Tree {
    final String value;

    public String val(){
        return value;
    }

    public IdentifierNode(String _val) {
        value = _val;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visitIdentifierNode(this);
    }
}

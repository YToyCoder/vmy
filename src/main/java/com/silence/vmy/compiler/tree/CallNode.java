package com.silence.vmy.compiler.tree;

import com.silence.vmy.compiler.visitor.NodeVisitor;

// call expression , it should be like : print("print")
public class CallNode implements Tree {
    final String identifier;
    final ListExpression params;

    public String identifier(){
        return identifier;
    }

    public ListExpression params(){
        return params;
    }

    public CallNode(String _identifier, ListExpression _params) {
        identifier = _identifier;
        params = _params;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visitCallNode(this);
    }
}

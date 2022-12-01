package com.silence.vmy.compiler.tree;

import com.silence.vmy.compiler.visitor.NodeVisitor;

public class Return implements Tree {
    final Tree value;

    public Tree val(){
        return value;
    }

    public Return(Tree _value) {
        value = _value;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visitReturn(this);
    }

}

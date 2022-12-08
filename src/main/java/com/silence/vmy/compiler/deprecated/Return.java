package com.silence.vmy.compiler.deprecated;

import com.silence.vmy.compiler.tree.Tree;
import com.silence.vmy.compiler.visitor.NodeVisitor;

public class Return extends AbstractTree implements Tree {
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

package com.silence.vmy.compiler.oldIR;

import com.silence.vmy.compiler.tree.Tree;
import com.silence.vmy.compiler.visitor.NodeVisitor;

public class ConditionNode extends AbstractTree implements Tree {
    final Tree condition;
    final BlockNode body;

    public Tree condition() { return condition; }
    public Tree body() { return body; }

    public ConditionNode(Tree _condition, BlockNode _body) {
        condition = _condition;
        body = _body;
    }

    @Override public void accept(NodeVisitor visitor) { }
}

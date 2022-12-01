package com.silence.vmy.compiler.tree;

import com.silence.vmy.compiler.visitor.NodeVisitor;

public class WhileLoop extends ConditionNode {
    public WhileLoop(Tree _cond, BlockNode _body) {
        super(_cond, _body);
    }


    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visitWhileLoop(this);
    }
}

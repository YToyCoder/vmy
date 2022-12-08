package com.silence.vmy.compiler.deprecated;

import com.silence.vmy.compiler.tree.Tree;
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

package com.silence.vmy.compiler.deprecated;

import com.silence.vmy.compiler.tree.Tree;
import com.silence.vmy.compiler.visitor.NodeVisitor;

public class BinaryOperatorNode extends AbstractTree implements Tree {
    final String OP;
    Tree left;
    Tree right;

    public Tree left(){
        return left;
    }

    public Tree right(){
        return right;
    }

    public String op(){
        return OP;
    }

    public BinaryOperatorNode(final String _op, final Tree _left, final Tree _right) {
        OP = _op;
        left = _left;
        right = _right;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visitBinaryOperator(this);
    }
}

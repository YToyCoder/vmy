package com.silence.vmy.compiler.tree;

public class ConditionNode implements Tree {
    final Tree condition;
    final BlockNode body;

    public Tree condition() {
        return condition;
    }

    public Tree body() {
        return body;
    }

    public ConditionNode(Tree _condition, BlockNode _body) {
        condition = _condition;
        body = _body;
    }
}

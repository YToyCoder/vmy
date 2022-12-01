package com.silence.vmy.compiler.tree;

import com.silence.vmy.compiler.visitor.NodeVisitor;

// represent an empty node
public class EmptyNode implements Tree {

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visitEmpty(this);
    }
}

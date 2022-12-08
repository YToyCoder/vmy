package com.silence.vmy.compiler.deprecated;

import com.silence.vmy.compiler.tree.Tree;
import com.silence.vmy.compiler.visitor.NodeVisitor;

// represent an empty node
public class EmptyNode extends AbstractTree implements Tree {

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visitEmpty(this);
    }
}

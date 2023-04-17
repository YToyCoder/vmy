package com.silence.vmy.compiler.oldIR;

import com.silence.vmy.compiler.visitor.NodeVisitor;

// represent an empty node
public class EmptyNode extends AbstractTree {
    @Override public void accept(NodeVisitor visitor) { visitor.visitEmpty(this); }
}

package com.silence.vmy.compiler.oldIR;

import com.silence.vmy.compiler.visitor.NodeVisitor;

public abstract class LiteralNode 
    extends AbstractTree {
    private final int tag;

    public LiteralNode(int _tag) { tag = _tag; }

    @Override public void accept(NodeVisitor visitor) { visitor.visitLiteralNode(this); }
    public abstract Object val();
}

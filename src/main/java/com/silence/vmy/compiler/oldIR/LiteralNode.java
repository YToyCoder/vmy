package com.silence.vmy.compiler.oldIR;

import com.silence.vmy.compiler.tree.Tree;
import com.silence.vmy.compiler.visitor.NodeVisitor;

public abstract class LiteralNode extends AbstractTree implements Tree {
    private final int tag;

    public LiteralNode(int _tag) {
        tag = _tag;
    }

    public abstract Object val();

//    public int tag() {
//        return tag;
//    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visitLiteralNode(this);
    }
}

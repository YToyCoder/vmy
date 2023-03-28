package com.silence.vmy.compiler.oldIR;

import com.silence.vmy.compiler.tree.Tree;
import com.silence.vmy.compiler.visitor.NodeVisitor;

public class ValNode extends AbstractTree implements Tree {
    private final Number value;

    public Number val() {
        return value;
    }

    public ValNode(Number _val) {
        value = _val;
    }

    @Override
    public void accept(NodeVisitor visitor) {
    }
}

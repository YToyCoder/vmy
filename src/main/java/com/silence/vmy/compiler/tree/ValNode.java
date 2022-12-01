package com.silence.vmy.compiler.tree;

import com.silence.vmy.compiler.visitor.NodeVisitor;

public class ValNode implements Tree {
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

package com.silence.vmy.compiler.oldIR;

import com.silence.vmy.compiler.visitor.NodeVisitor;

public class ValNode extends AbstractTree {
    private final Number value;

    public Number val() { return value; }
    @Override public void accept(NodeVisitor visitor) { }

    public ValNode(Number _val) { value = _val; }
}

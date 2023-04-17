package com.silence.vmy.compiler.oldIR;

import com.silence.vmy.compiler.tree.Tree;
import com.silence.vmy.compiler.visitor.NodeVisitor;

import java.util.List;

// function declaration
public class FunctionNode extends AbstractTree {
    final List<DeclareNode> params;
    final Tree body;
    final String name;

    public List<DeclareNode> params() { return params; }
    public Tree body() { return body; }
    public String name() { return name; }
    @Override public void accept(NodeVisitor visitor) { visitor.visitFunction(this); }

    public FunctionNode(String _name, List<DeclareNode> _params, Tree _body) {
        // the last one is return type
        params = _params;
        body = _body;
        name = _name;
    }
}

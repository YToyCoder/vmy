package com.silence.vmy.compiler.oldIR;

import com.silence.vmy.compiler.tree.Tree;
import com.silence.vmy.compiler.visitor.NodeVisitor;

// node for Declaration, like let a : Type , val a : Type
public class DeclareNode extends AbstractTree implements Tree {
    final String declare;
    final String type;
    final IdentifierNode identifier;

    public String declare() { return declare; }
    public String type()    { return type; }
    public IdentifierNode identifier() { return identifier; }
    @Override public void accept(NodeVisitor visitor) { visitor.visitDeclareNode(this); }

    public DeclareNode(String _declare, IdentifierNode _identifier) { this(_declare, _identifier, null); }
    public DeclareNode(String _declare, IdentifierNode _identifier, String _type) {
        declare = _declare;
        identifier = _identifier;
        type = _type;
    }

}

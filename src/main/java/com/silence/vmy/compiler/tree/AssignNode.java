package com.silence.vmy.compiler.tree;

import com.silence.vmy.compiler.visitor.NodeVisitor;

// node for assignment
// like :
//      let a : Type = 1
//      a = 2
public class AssignNode implements Tree {
    Tree variable;
    Tree expression;

    public AssignNode(Tree _variable, Tree expr) {
        variable = _variable;
        expression = expr;
    }

    public Tree variable(){
        return variable;
    }

    public Tree expression(){
        return expression;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visitAssignment(this);
    }
}

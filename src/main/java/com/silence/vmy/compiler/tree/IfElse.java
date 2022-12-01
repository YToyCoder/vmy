package com.silence.vmy.compiler.tree;

import com.silence.vmy.compiler.visitor.NodeVisitor;

import java.util.List;
import java.util.Objects;

public class IfElse implements Tree {
    final ConditionNode TheIf;
    final List<ConditionNode> Elif;
    final Tree Else;

    public ConditionNode theIf(){
        return TheIf;
    }

    public List<ConditionNode> elif(){
        return Elif;
    }

    public Tree _else(){
        return Else;
    }

    public IfElse(ConditionNode _if, List<ConditionNode> _else_conditions, Tree _else) {
        TheIf = Objects.requireNonNull(_if);
        Elif = _else_conditions;
        Else = _else;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visitIfElse(this);
    }
}

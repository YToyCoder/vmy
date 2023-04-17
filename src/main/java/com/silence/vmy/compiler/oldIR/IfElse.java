package com.silence.vmy.compiler.oldIR;

import com.silence.vmy.compiler.tree.Tree;
import com.silence.vmy.compiler.visitor.NodeVisitor;

import java.util.List;
import java.util.Objects;

public class IfElse
    extends AbstractTree{

    final ConditionNode TheIf;
    final List<ConditionNode> Elif;
    final Tree Else;

    public ConditionNode theIf()        { return TheIf; }
    public List<ConditionNode> elif()   { return Elif; }
    public Tree _else()                 { return Else; }
    @Override public void accept(NodeVisitor visitor) { visitor.visitIfElse(this); }

    public IfElse(ConditionNode _if, List<ConditionNode> _else_conditions, Tree _else) {
        TheIf = Objects.requireNonNull(_if);
        Elif = _else_conditions;
        Else = _else;
    }

}

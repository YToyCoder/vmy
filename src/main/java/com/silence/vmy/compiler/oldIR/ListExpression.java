package com.silence.vmy.compiler.oldIR;

import com.silence.vmy.compiler.tree.Tree;
import com.silence.vmy.compiler.visitor.NodeVisitor;

import java.util.List;

// a list expression should be like this below:
// a, b, c  or print(a, b, c)
public class ListExpression extends AbstractTree {
    final List<Tree> elements;

    public List<Tree> elements() { return elements; }
    @Override public void accept(NodeVisitor visitor) { }

    public ListExpression(List<Tree> _els) { elements = _els; }
}

package com.silence.vmy.compiler.tree;

import java.util.List;

// a list expression should be like this below:
// a, b, c  or print(a, b, c)
public class ListExpression implements Tree {
    final List<Tree> elements;

    public List<Tree> elements() {
        return elements;
    }

    public ListExpression(List<Tree> _els) {
        elements = _els;
    }
}

package com.silence.vmy.compiler.tree;

import com.silence.vmy.compiler.visitor.NodeVisitor;

public interface Tree<T> {

    // visitor pattern
    void accept(NodeVisitor visitor);
    T accept(TreeVisitor<? extends T> visitor);

}

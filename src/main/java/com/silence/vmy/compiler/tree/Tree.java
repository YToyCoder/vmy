package com.silence.vmy.compiler.tree;

import com.silence.vmy.compiler.visitor.NodeVisitor;

public interface Tree {

    // old version
    default void accept(NodeVisitor visitor){
        accept(visitor);
    }

    // new version
    <R,T> R accept(TreeVisitor<R,T> visitor, T payload);

}

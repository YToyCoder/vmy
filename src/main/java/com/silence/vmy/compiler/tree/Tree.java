package com.silence.vmy.compiler.tree;

import com.silence.vmy.compiler.visitor.ASTProcessingException;
import com.silence.vmy.compiler.visitor.NodeVisitor;

public interface Tree {

    default void accept(NodeVisitor visitor) {
        throw new ASTProcessingException("your should override this method(%s)".formatted(this.getClass().getName()));
    }

}

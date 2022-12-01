package com.silence.vmy.compiler.tree;

import com.silence.vmy.compiler.visitor.NodeVisitor;

import java.util.List;

/**
 * a code block, like:
 * <p>let a : Int </p>
 * <p>a = 1 </p>
 * <p>print(a)</p>
 */
public class BlockNode implements Tree {
    List<Tree> process;

    public List<Tree> process(){
        return process;
    }
    public BlockNode(List<Tree> _process) {
        process = _process;
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visitBlockNode(this);
    }
}

package com.silence.vmy.compiler.deprecated;

import com.silence.vmy.compiler.tree.Tree;
import com.silence.vmy.compiler.tree.TreeVisitor;
import com.silence.vmy.compiler.visitor.NodeVisitor;

public class AbstractTree implements Tree {
  @Override
  public void accept(NodeVisitor visitor) {
  }

  @Override
  public Object accept(TreeVisitor visitor) {
    return null;
  }
}

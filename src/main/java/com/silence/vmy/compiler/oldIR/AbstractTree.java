package com.silence.vmy.compiler.oldIR;

import com.silence.vmy.compiler.tree.TVisitor;
import com.silence.vmy.compiler.tree.Tree;
import com.silence.vmy.compiler.tree.TreeVisitor;
import com.silence.vmy.compiler.visitor.NodeVisitor;

public class AbstractTree implements Tree {
  @Override
  public void accept(NodeVisitor visitor) {
  }

  public Object accept(TreeVisitor<?,?> visitor) {
    return null;
  }

  @Override
  public <R, T> R accept(TVisitor<R> visitor, T t){
    return null;
  }

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return null;
  }

  @Override
  public long position() {
    return 0;
  }

  @Override
  public Tag tag() {
    return null;
  }
}

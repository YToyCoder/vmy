package com.silence.vmy.compiler.tree;

public class BlockStatement implements Statement{
  @Override
  public <R,T> R accept(TreeVisitor<R,T> visitor, T payload) {
    return visitor.visitBlock(this, payload);
  }
}

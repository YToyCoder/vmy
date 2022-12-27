package com.silence.vmy.compiler.tree;

import java.util.List;

public record BlockStatement(List<Tree> exprs, long position) implements Statement{
  @Override
  public <R,T> R accept(TreeVisitor<R,T> visitor, T payload) {
    return visitor.visitBlock(this, payload);
  }

  @Override
  public Tag tag() {
    return null;
  }
}

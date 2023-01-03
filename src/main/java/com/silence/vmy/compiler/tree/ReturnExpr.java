package com.silence.vmy.compiler.tree;

public record ReturnExpr(long position, Tag tag, Tree body) implements Expression {
  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitReturnExpr(this, payload);
  }
}

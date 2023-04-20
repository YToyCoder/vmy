package com.silence.vmy.compiler.tree;

public record ReturnExpr(long position, Tag tag, Tree body) implements Expression {
  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitReturnExpr(this, payload);
  }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    if(visitor.enterReturn(this, t))
      return visitor.leaveReturn(this, t);
    return this;
  }

  @Override
  public String toString() {
    return "ret => " + body;
  }
}

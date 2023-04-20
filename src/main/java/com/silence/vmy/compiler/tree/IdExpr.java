package com.silence.vmy.compiler.tree;

public record IdExpr(long position, Tag tag, String name) implements Expression {
  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitIdExpr(this, payload);
  }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    if(visitor.enterIdExpr(this, t))
      return visitor.leaveIdExpr(this, t);
    return this;
  }
  @Override 
  public String toString() {
    return "id(" + name + ")";
  }
}

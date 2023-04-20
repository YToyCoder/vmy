package com.silence.vmy.compiler.tree;

public class Unary extends OperatorExpression{
  private final Tag tag;
  private final Tree body;

  public Unary(Tag tag, Tree body) {
    this.tag = tag;
    this.body = body;
  }

  public Tree body(){ return body; }

  @Override
  public <R,T> R accept(TreeVisitor<R,T> visitor, T payload) { return visitor.visitUnary(this, payload); }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    if(visitor.enterUnary(this, t))
      return visitor.leaveUnary(this, t);
    return this;
  }

  @Override public Tag tag() { return tag; }
  @Override 
  public String toString() { 
    return "" + tag + "|" + body;
  }
}

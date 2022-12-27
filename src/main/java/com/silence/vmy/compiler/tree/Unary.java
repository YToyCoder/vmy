package com.silence.vmy.compiler.tree;

public class Unary extends OperatorExpression{
  private final Tag tag;
  private final Tree body;

  public Unary(Tag tag, Tree body) {
    this.tag = tag;
    this.body = body;
  }

  public Tree body(){
    return body;
  }

  @Override
  public <R,T> R accept(TreeVisitor<R,T> visitor, T payload) {
    return visitor.visitUnary(this, payload);
  }

  @Override
  public Tag tag() {
    return tag;
  }
}

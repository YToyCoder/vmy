package com.silence.vmy.compiler.tree;

public class BinaryOperateExpression extends OperatorExpression{
  private final Expression lhe; // left hand expression
  private final Expression rhe; // right hand expression
  private final Tag tag;

  public BinaryOperateExpression(Expression lhe, Expression rhe, Tag tag) {
    this.lhe = lhe;
    this.rhe = rhe;
    this.tag = tag;
  }

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitBinary(this, payload);
  }

  public Expression left(){
    return lhe;
  }

  public Expression right(){
    return rhe;
  }

  @Override
  public Tag tag() {
    return null;
  }
}

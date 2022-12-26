package com.silence.vmy.compiler.tree;

public class BinaryOperateExpression extends OperatorExpression{
  private final Expression lhe; // left hand expression
  private final Expression rhe; // right hand expression

  public BinaryOperateExpression(Expression lhe, Expression rhe) {
    this.lhe = lhe;
    this.rhe = rhe;
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
}

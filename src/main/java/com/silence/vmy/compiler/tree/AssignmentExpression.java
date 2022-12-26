package com.silence.vmy.compiler.tree;

public record AssignmentExpression(Expression left, Expression right) implements Expression{

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitAssignment(this, payload);
  }
}

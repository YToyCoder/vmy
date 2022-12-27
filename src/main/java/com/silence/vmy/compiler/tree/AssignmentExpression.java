package com.silence.vmy.compiler.tree;

public record AssignmentExpression(Expression left, Expression right, long position) implements Expression{

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitAssignment(this, payload);
  }

  @Override
  public Tag tag() {
    return Tag.Assign;
  }
}

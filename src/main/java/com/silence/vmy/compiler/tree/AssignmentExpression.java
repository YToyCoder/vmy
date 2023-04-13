package com.silence.vmy.compiler.tree;

public record AssignmentExpression(Expression left, Expression right, long position) implements Expression{

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitAssignment(this, payload);
  }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    if(!visitor.enterAssignment(this, t))
      return this;
    return visitor.leaveAssignment(
        setLeft((Expression) left.accept(visitor, t))
        .setRight((Expression) right.accept(visitor, t)),
        t);
  }

  private AssignmentExpression setLeft(Expression expression){
    if(expression == left) {
      return this;
    }
    return new AssignmentExpression(expression, right, position);
  }

  private AssignmentExpression setRight(Expression expression){
    if(expression == right) {
      return this;
    }
    return new AssignmentExpression(left, expression, position);
  }

  @Override
  public Tag tag() {
    return Tag.Assign;
  }
}

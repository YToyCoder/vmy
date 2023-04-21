package com.silence.vmy.compiler.tree;

public class BinaryOperateExpression extends OperatorExpression{
  private final Expression lhe; // left hand expression
  private final Expression rhe; // right hand expression
  private final Tag tag;

  public BinaryOperateExpression(Expression lhe, Expression rhe, Tag tag) {
    this(lhe, rhe, tag, 0);
  }
  public BinaryOperateExpression(Expression lhe, Expression rhe, Tag tag,long position) {
    this.lhe = lhe;
    this.rhe = rhe;
    this.tag = tag;
    setPos(position);
  }

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) { return visitor.visitBinary(this, payload); }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    if(visitor.enterBinary(this, t)) {
      return visitor.leaveBinary(
          setLeft((Expression) left().accept(visitor, t))
          .setRight((Expression) right().accept(visitor, t)),
          t);
    }
    return this;
  }

  private BinaryOperateExpression setLeft(Expression expression) {
    if(expression != this.lhe)
      return new BinaryOperateExpression(expression, right(), tag, position());
    return this;
  }

  private BinaryOperateExpression setRight(Expression expression) {
    if(expression != this.rhe)
      return new BinaryOperateExpression(left(), expression, tag, position());
    return this;
  }

  public Expression left() { return lhe; }
  public Expression right(){ return rhe; }
  @Override public Tag tag() { return tag; }
  public String toString() { return "%s [%s] %s".formatted(lhe.toString(), tag.toString(), rhe.toString()); }
}

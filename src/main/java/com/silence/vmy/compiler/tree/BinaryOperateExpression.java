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
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) { return visitor.visitBinary(this, payload); }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    if(visitor.enterBinary(this, t)) {
      return visitor.leaveBinary(
          setLhe((Expression) lhe.accept(visitor, t))
          .setRhe((Expression) right().accept(visitor, t)),
          t);
    }
    return this;
  }

  private BinaryOperateExpression setLhe(Expression expression) {
    if(expression != this.lhe)
      return new BinaryOperateExpression(expression, right(), tag);
    return this;
  }

  private BinaryOperateExpression setRhe(Expression expression) {
    if(expression != this.rhe)
      return new BinaryOperateExpression(left(), expression, tag);
    return this;
  }

  public Expression left() { return lhe; }
  public Expression right(){ return rhe; }
  @Override public Tag tag() { return tag; }
  public String toString() { return "Binary[left=%s,right=%s,tag=%s]".formatted(lhe.toString(), rhe.toString(), tag.toString()); }
}

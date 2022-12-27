package com.silence.vmy.compiler.tree;

public class LiteralExpression extends BaseTree implements Expression{

  public LiteralExpression(Kind kind) {
    this.kind = kind;
  }

  @Override
  public <R,T> R accept(TreeVisitor<R,T> visitor, T payload) {
    return visitor.visitLiteral(this, payload);
  }

  @Override
  public BaseTree.Tag tag() {
    return null;
  }

  public enum Kind {
    Int,
    String,
    Boolean
  }

  private final Kind kind;

  public boolean isInt(){
    return kind == Kind.Int;
  }

  public boolean isBoolean(){
    return kind == Kind.Boolean;
  }

  public boolean isString(){
    return kind == Kind.String;
  }

}

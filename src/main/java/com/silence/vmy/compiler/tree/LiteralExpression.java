package com.silence.vmy.compiler.tree;

import com.silence.vmy.compiler.visitor.NodeVisitor;

public class LiteralExpression implements Expression{

  public LiteralExpression(Tag tag) {
    this.tag = tag;
  }

  @Override
  public <R,T> R accept(TreeVisitor<R,T> visitor, T payload) {
    return visitor.visitLiteral(this, payload);
  }

  public enum Tag{
    Int,
    String,
    Boolean
  }

  private final Tag tag;

  public boolean isInt(){
    return tag == Tag.Int;
  }

  public boolean isBoolean(){
    return tag == Tag.Boolean;
  }

  public boolean isString(){
    return tag == Tag.String;
  }

}

package com.silence.vmy.compiler.tree;

public record FunctionDecl() implements Expression{
  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitFunctionDecl(this, payload);
  }
}

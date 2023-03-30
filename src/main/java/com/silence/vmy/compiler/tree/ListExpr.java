package com.silence.vmy.compiler.tree;

import java.util.List;

public record ListExpr<E extends Expression>(long position, Tag tag, List<E> body) implements Expression{

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitListExpr(this, payload);
  }
  
}

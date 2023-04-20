package com.silence.vmy.compiler.tree;

import java.util.List;

public record ListExpr<E extends Expression>(long position, Tag tag, List<E> body) implements Expression{

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitListExpr(this, payload);
  }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    return null;
  }

  @Override public
  String toString() {
    StringBuilder sb = new StringBuilder("(");
    for(E el : body) {
      sb.append(el + ",");
    }
    return sb.append(")").toString();
  }

}

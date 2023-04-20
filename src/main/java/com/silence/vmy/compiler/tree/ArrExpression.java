package com.silence.vmy.compiler.tree;

import java.util.List;

public record ArrExpression(
  List<Expression> elements, 
  Tag tag, 
  long position
  ) implements Expression{

  @Override public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) { return visitor.visitArr(this, payload); }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    if(visitor.enterArrExpression(this, t))
      return visitor.leaveArrExpression(this, t);
    return this;
  }

  @Override public
  String toString() {
    StringBuilder sb = new StringBuilder("[");
    for(var el : elements) {
      sb.append(el + ",\n");
    }
    return sb.append("]").toString();
  }
  
}

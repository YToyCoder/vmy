package com.silence.vmy.compiler.tree;

import java.util.List;
import java.util.ArrayList;

public record ArrExpression(
  List<Expression> elements, 
  Tag tag, 
  long position
  ) implements Expression{

  @Override public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) { return visitor.visitArr(this, payload); }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    if(visitor.enterArrExpression(this, t))
      return visitor.leaveArrExpression(handleElements(visitor, t), t);
    return this;
  }

  private <T> ArrExpression handleElements(TVisitor<T> visitor, T t)
  {
    List<Expression> elems = new ArrayList<>(elements.size());
    boolean anyChanged = false;
    for(Expression el : elements){
      var mayChanged = (Expression)el.accept(visitor, t);
      if(mayChanged != el){
        anyChanged = true;
      }
      elems.add(mayChanged);
    }
    return anyChanged ? new ArrExpression(elems, tag, position) : this;
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

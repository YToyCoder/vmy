package com.silence.vmy.compiler.tree;

import java.util.ArrayList;
import java.util.List;

public record CallExpr(
    long position,
    Tag tag,
    String callId,
    ListExpr<? extends Expression> params
) implements Expression {

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitCallExpr(this, payload);
  }
  @Override public 
  String toString() {
    return "" + callId + "(" + params + ")";
  }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    return visitor.enterCallExpr(this, t) ?
        visitor.leaveCallExpr(doWithList(visitor, t), t) :
        this;
  }

  private <T> CallExpr doWithList(TVisitor<T> visitor, T t){
    boolean anyChanged = false;
    List<Expression> ret = new ArrayList<>(params.body().size());
    for(Expression el : params.body()){
      Expression mayChanged = (Expression) el.accept(visitor, t);
      if(mayChanged != el){
        anyChanged = true;
      }
      ret.add(mayChanged);
    }
    return anyChanged ?
        new CallExpr(position, tag, callId, new ListExpr<>(params.position(), params.tag(), ret)) :
        this;
  }

  public static CallExpr create(long pos, String callId, ListExpr<? extends Expression> params){
    return new CallExpr(pos, Tag.CallExpr, callId, params);
  }
}

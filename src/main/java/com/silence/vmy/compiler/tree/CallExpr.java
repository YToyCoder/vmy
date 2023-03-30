package com.silence.vmy.compiler.tree;

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

  public static CallExpr create(long pos, String callId, ListExpr<? extends Expression> params){
    return new CallExpr(pos, Tag.CallExpr, callId, params);
  }
}

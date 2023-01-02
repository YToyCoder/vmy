package com.silence.vmy.compiler.tree;

import java.util.List;

public record ListExpr(long position, Tag tag, List<? extends Expression> body) implements Expression{

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitListExpr(this, payload);
  }
  
}

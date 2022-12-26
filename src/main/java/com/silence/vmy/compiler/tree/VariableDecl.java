package com.silence.vmy.compiler.tree;

import com.silence.vmy.compiler.Modifiers;

public record VariableDecl(String name, Modifiers modifiers) implements Expression {

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitVariableDecl(this, payload);
  }
}

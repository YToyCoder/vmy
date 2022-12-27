package com.silence.vmy.compiler.tree;

public record FunctionDecl(long position) implements Tree{
  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitFunctionDecl(this, payload);
  }

  @Override
  public Tag tag() {
    return Tag.Fun;
  }
}

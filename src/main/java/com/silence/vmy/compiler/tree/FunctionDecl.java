package com.silence.vmy.compiler.tree;

import java.util.List;

public record FunctionDecl(
    String name,
    List<VariableDecl> params,
    TypeExpr ret,
    BlockStatement body,
    long position
) implements Tree{

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitFunctionDecl(this, payload);
  }

  @Override
  public Tag tag() {
    return Tag.Fun;
  }
}

package com.silence.vmy.compiler.tree;

import java.util.List;

public record FunctionDecl(
    String name,
    List<VariableDecl> params,
    TypeExpr ret,
    BlockStatement body,
    long position
) implements Statement{

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitFunctionDecl(this, payload);
  }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    return null;
  }

  @Override
  public Tag tag() {
    return Tag.Fun;
  }
}

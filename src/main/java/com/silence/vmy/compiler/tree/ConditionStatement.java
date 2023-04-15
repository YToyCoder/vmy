package com.silence.vmy.compiler.tree;

public record ConditionStatement(
    Tree condition,
    BlockStatement block,
    Tag tag,
    long position
    ) implements Statement{
  /* no visiting method directly */
  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) { return null; }
  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) { return null; }
}

package com.silence.vmy.compiler.tree;

import java.util.List;

public record IfStatement(
    ConditionStatement ifStatement,
    List<ConditionStatement> elif,
    BlockStatement el
    ) implements Statement {

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) { return visitor.visitIfStatement(this, payload); }
  @Override
  public Tag tag() { return Tag.If; }
  @Override
  public long position() { return ifStatement.position(); }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    if(visitor.enterIfStatement(this, t))
      return visitor.leaveIfStatement(this, t);
    return this;
  }

}

package com.silence.vmy.compiler.tree;

import com.silence.vmy.compiler.Modifiers;

public record VariableDecl(String name, Modifiers modifiers, TypeExpr t, long position) implements Statement{

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) { return visitor.visitVariableDecl(this, payload); }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    if(visitor.enterVariableDecl(this, t))
      return visitor.leaveVariableDecl(this, t);
    return this;
  }


  @Override
  public Tag tag() { return Tag.VarDecl; }
  @Override public String toString() { return "" + name + ":" + (t == null ? "?" : t.typeId());}
}

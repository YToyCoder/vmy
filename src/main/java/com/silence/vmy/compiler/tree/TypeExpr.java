package com.silence.vmy.compiler.tree;

public record TypeExpr(long position, Tag tag, String typeId) implements Expression {
  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitTypeExpr(this, payload);
  }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    if(visitor.enterTypeExpr(this, t))
      return visitor.leaveTypeExpr(this, t);
    return this;
  }

  public static TypeExpr create(long pos, String typeId) { return new TypeExpr(pos, Tag.TypeDecl, typeId); }

}

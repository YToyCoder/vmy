package com.silence.vmy.compiler.tree;

import java.util.List;
/*
 * for-statement
 * two case :
 * 1. in-index-iterator
 * "for" id","id "in" id block
 * 2. in-no-index-iterator
 * "for" id "in" id block
 **/
public record ForStatement(
  ForKind forkind, 
  List<IdExpr> heads, 
  IdExpr arrId, 
  BlockStatement body, 
  long position
  ) implements Statement {

  public ForStatement(
    List<IdExpr> heads, 
    IdExpr arrId, 
    BlockStatement body, 
    long position
    ) { this(ForKind.NoIndex, heads, arrId, body, position); }

  @Override public Tag tag() { return Tag.For; }
  public enum ForKind {WithIndex, NoIndex; }
  public boolean isWithIndex() { return forkind() == ForKind.WithIndex; }
  @Override public 
  <R,T> R accept(TreeVisitor<R,T> visitor, T payload) { return visitor.visitForStatement(this, payload); }
  @Override public <T> Tree accept(TVisitor<T> visitor, T t) { return null; } // visitor.visitForStatement(this, payload); }
  public static ForStatement withIndex(List<IdExpr> heads, IdExpr arrId, BlockStatement body, long position) { 
    return new ForStatement(ForKind.WithIndex, heads, arrId, body, position); 
  }
  public static ForStatement withoutIndex(List<IdExpr> heads, IdExpr arrId, BlockStatement body, long position) { 
    return new ForStatement(heads, arrId, body, position); 
  }
}

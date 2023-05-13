package com.silence.vmy.compiler.tree;

import java.util.List;

public class ExportState implements Statement
{

  private long position;
  private List<ExportExp> exports;
  private Kind _kind;

  private ExportState(long position, List<ExportExp> exports, Kind _kind) {
    this.position = position;
    this.exports = exports;
    this._kind = _kind;
  }

  public static ExportState create(ExportExp exp, long position){
    return new ExportState(position, List.of(exp), Kind.One);
  }
  public static ExportState create(List<ExportExp> exp, long position){
    return new ExportState(position, exp, Kind.Obj);
  }

  private static enum Kind {
    One,
    Obj
  }

  public boolean isOne() { return _kind == Kind.One; }
  public boolean isObjForm() { return _kind == Kind.Obj; }
  public ExportExp getOne(){
    return exports.size() > 0 ? exports.get(0) : null;
  }
  public List<ExportExp> getAll(){ return exports; }

  public static record 
  ExportExp(String name, String alias, boolean hasAlias, long position) {}

  public static ExportExp createExport(String name, long position){
    return new ExportExp(name, "", false, position);
  }

  public static ExportExp createExport(String name, String alias, long position){
    return new ExportExp(name, alias, true, position);
  }

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    throw new UnsupportedOperationException("Unimplemented method 'accept'");
  }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    if(visitor.enterExport(this, t))
      return visitor.leaveExport(this, t);
    return this;
  }

  @Override public long position() { return position; }
  @Override public Tag tag() { return Tag.Export; }
}

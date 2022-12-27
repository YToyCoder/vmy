package com.silence.vmy.compiler.tree;

public abstract class BaseTree implements Tree{
  private long pos;

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return null;
  }

  public BaseTree setPos(long pos){
    this.pos = pos;
    return this;
  }

  @Override
  public long position() {
    return pos;
  }

}

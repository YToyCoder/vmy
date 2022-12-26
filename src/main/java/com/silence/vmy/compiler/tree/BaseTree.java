package com.silence.vmy.compiler.tree;

public abstract class BaseTree implements Tree{
  public long position;

  public BaseTree setPosition(long position) {
    this.position = position;
    return this;
  }

}

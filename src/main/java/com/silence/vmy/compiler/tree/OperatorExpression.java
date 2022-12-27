package com.silence.vmy.compiler.tree;

public abstract class OperatorExpression extends BaseTree implements Expression{
  public enum OperandPos {
    Left,
    Right;
  }

}

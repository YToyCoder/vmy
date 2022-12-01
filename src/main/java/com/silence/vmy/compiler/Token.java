package com.silence.vmy.compiler;

public class Token {
  public final int tag;
  public final String value;
  public final int pos;
  public Token(int _tag, String val, int pos){
    tag = _tag;
    value = val;
    this.pos = pos;
  }
  public Token(int _tag, String val){
    this(_tag, val, -1);
  }

  @Override
  public String toString() {
    return "Token{" +
        "tag=" + tag +
        ", value='" + value + '\'' +
        ", pos=" + pos +
        '}';
  }

  public static final int INT_V = 0;
  public static final int DOUBLE_V = 1;
  public static final int Identifier = 2;
  public static final int Assignment = 3;
  public static final int NewLine = 4;
  public static final int Declaration = 5;
  public static final int Literal = 6;

  public static final int BuiltinCall = 7;
  public static final int Builtin = 9;
  public static final int Comma = 8;
}

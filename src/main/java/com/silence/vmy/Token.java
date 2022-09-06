package com.silence.vmy;

public class Token {
  final int tag;
  final String value;
  final int pos;
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

  static final int INT_V = 0;
  static final int DOUBLE_V = 1;
  static final int Identifier = 2;
  static final int Assignment = 3;
  static final int NewLine = 4;
  static final int Declaration = 5;
  static final int Literal = 6;

  static final int BuiltinCall = 7;
  static final int Builtin = 9;
  static final int Comma = 8;
}

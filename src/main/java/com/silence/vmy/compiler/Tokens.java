package com.silence.vmy.compiler;

public class Tokens {
  public record Token(TokenKind kind, long start, long end, String payload) {
    public Token(TokenKind kind, long start, long end){
      this(kind, start, end, null);
    }
  }

  public static long location(int row, int col){
    // 8 * 8 => 32 | 16
    assert row >= 0 : "row must be >= 0 and <= Integer.MAX_VALUE, it's %d".formatted(row);
    assert col >= 0 : "col must be >= 0 and <= Integer.MAX_VALUE, it's %d".formatted(col);
    return ((long)row) << 32 | col;
  }

  public enum TokenKind {
    newline,
    EOF,
    // literal
    IntLiteral,
    DoubleLiteral,
    StringLiteral,
    CharLiteral,
    True,
    False,
    // control flow
    For,
    While,
    If,
    Elif,
    Else,
    // declaration
    Let, // let
    Val, // val
    Fun, // fun
    Id, // identifier
    Assignment, // =
    // pair
    LParenthesis, // (
    RParenthesis, // )
    LBrace, // {
    RBrace, // }
    Quote, // "
    // calculate
    Add, // +
    AddEqual, // +=
    Sub, // -
    SubEqual, // -=
    Multi, // *
    MultiEqual, // *=
    Div, // /
    DivEqual, // /=
    Equal, // ==
    NotEqual, // !=
    Less, // <
    Greater, // >
    Le, // <=
    Ge, // >=
    // misc
    Return, // return
    Colon, // :
    Comma, // ,
    Dot, // .
    Annotation, // #
  }

}

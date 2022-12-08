package com.silence.vmy.compiler;

public interface Lexer {
  Tokens.Token next();
  boolean hasNext();
  Tokens.Token peek();
  Tokens.Token last();
}

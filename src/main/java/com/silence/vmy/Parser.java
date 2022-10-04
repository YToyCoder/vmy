package com.silence.vmy;

/**
 * root interface for parsing tokens to Abstract Syntax tree
 */
public interface Parser {
  AST.Tree parse();
}

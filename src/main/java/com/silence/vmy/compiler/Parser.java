package com.silence.vmy.compiler;

import com.silence.vmy.compiler.tree.Root;

/**
 * root interface for parsing tokens to Abstract Syntax tree
 */
public interface Parser {
  Root parse();
  default String file_name() { return ""; }
}

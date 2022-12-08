package com.silence.vmy.compiler.tree;

public interface VariableTree {
  boolean is_constant();
  String getName();
  Tree getType();
  Expression getInitializer();
}

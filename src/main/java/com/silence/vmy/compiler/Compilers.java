package com.silence.vmy.compiler;

import com.silence.vmy.compiler.tree.*;

public abstract class Compilers 
{
  // compileUnit rep
  public static interface CompileUnit 
  {
    boolean compiled();
    Tree node();
  }
}


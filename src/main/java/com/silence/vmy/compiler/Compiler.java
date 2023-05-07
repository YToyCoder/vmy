package com.silence.vmy.compiler;

public interface Compiler<T> 
{
  Compilers.CompileUnit compile(T context, Compilers.CompileUnit unit);
}

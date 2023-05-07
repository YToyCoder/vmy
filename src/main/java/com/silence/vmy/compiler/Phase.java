package com.silence.vmy.compiler;

public interface Phase<T extends Context> 
{
  Compilers.CompileUnit run(T contex, Compilers.CompileUnit unit);
}

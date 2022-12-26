package com.silence.vmy.compiler;

import com.silence.vmy.compiler.tree.*;

import java.util.List;
import java.util.Stack;

public class GeneralParser implements Parser{
  private Lexer lexer;
  private Stack<BaseTree> nodes;
  @Override
  public Root parse() {
    return null;
  }

  /**
   * e_fun = fun identifier expr "{" e_block "}"
   */
  private FunctionDecl compileFunc(){
    return null;
  }

  /**
   * expr = "(" ")" | "(" expr2 ")"
   */
  private Expression expr(){
    return null;
  }

  /**
   * expr2 =  expr3 | expr2 "," expr3
   */
  private List<Exception> expr2(){
    return null;
  }

  /**
   * e_block = [ expression ]
   */
  private BlockStatement compileBlock(){
    return null;
  }

  /**
   * expr3 = varDecl "=" expr4
   */
  private Expression expr3(){
    return null;
  }

  /**
   * call = identifier expr
   */
  private Expression call(){
    return null;
  }

}

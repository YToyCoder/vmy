package com.silence.vmy.tools;

import com.silence.vmy.compiler.*;
import com.silence.vmy.compiler.transform.IrTransforms;
import com.silence.vmy.compiler.tree.Root;
import com.silence.vmy.compiler.tree.Trees;
import com.silence.vmy.runtime.Evaluator;
import com.silence.vmy.runtime.Evaluators;

import java.io.FileNotFoundException;
import java.util.Objects;
import java.util.Scanner;

public class Eval {

  private static final String notice = """
      Hell , welcome to vmy!
      version 0.1
      """;
  public static void repl(){ repl(Evaluators.evaluator(true)); }

  public static void repl(final Evaluator evaluator){
    System.out.println(notice);
    String input = "";
    Scanner scanner = new Scanner(System.in);
    while(!Objects.equals(input, "#")){
      System.out.print("> ");
      input = scanner.nextLine();

      if(input.trim().length() == 0 ) continue;
      if(Objects.equals(input, "#")){
        scanner.close();
        System.exit(0);
      }

      try{
        Root parsing = parsing(input, false);
        Root transformedIr = transformToOldTree(parsing);
        Object ans = evaluator.eval(transformedIr);
        if(Objects.nonNull(ans))
          Utils.log(ans.toString());
      }catch (Exception e){
        Utils.error(e.getMessage());
      }
    }
  }
  /**
   * assign specific {@link Evaluator}
   * @param expression vmy language expression like : let a = 1
   * @param evaluator {@link Evaluator}
   * @return evaluate result like : 1 + 2 -> 3
   */
  public static Object eval(final String expression, final Evaluator evaluator){
    return Scripts.run_with_file_input_scanner(
            expression,
            false,
            scanner -> // do with scanner
                evaluator.eval(
                    SimpleParser
                        .create(scanner)
                        .parse()));
  }

  public static Root parsing(String filenameOrCode, boolean isFile) 
    throws FileNotFoundException {
    return parsing(filenameOrCode, isFile, false);
  }
  public static Root parsing(String filenameOrCode, boolean isFile, boolean debug) 
    throws FileNotFoundException {
    return GeneralParser
      .create(new GeneralScanner(filenameOrCode, isFile), debug)
      .parse();
  }

  public static Object eval(String filenameOrCode, boolean is_file) {
    try {
      Root parsing = parsing(filenameOrCode, is_file);
      return Evaluators.evaluator(true).eval(transformToOldTree(parsing));
    }catch (Exception e){
      throw new RuntimeException(e);
    }
  }

  public static void evalRoot(Root unit){
    try {
      Evaluators.evaluator(true).eval(transformToOldTree(unit));
    }catch(Exception e){
      throw new RuntimeException(e);
    }
  }

  private static Root transformToOldTree(Root root){
    if(root instanceof AST.VmyAST)
      return root;
    else if(root instanceof Trees.CompileUnit)
      return (Root) root.accept(new IrTransforms.Convert2OldIR(), null);
    else 
      throw new RuntimeException("not support transforming tree");
  }

}

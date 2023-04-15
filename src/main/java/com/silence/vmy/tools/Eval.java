package com.silence.vmy.tools;

import com.silence.vmy.compiler.*;
import com.silence.vmy.compiler.transform.IrTransforms;
import com.silence.vmy.compiler.tree.Root;
import com.silence.vmy.runtime.Evaluator;
import com.silence.vmy.runtime.Evaluators;

import java.io.FileNotFoundException;
import java.util.Objects;
import java.util.Scanner;

public class Eval {
  // eval an expression , like 1 + 2 * (3 + 4)
  public static Object eval(final String expression){
    return 
    Evaluators.defaultTreeEvaluator().eval(
      AST.build(
        Scanners.scanner(expression)
      )
    );
  }

  private static final String notice = """
      Hell , welcome to vmy!
      version 0.1
      """;
  public static void repl(){ repl(Evaluators.variableStoreTreeEvaluator()); }

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
        Object ans = eval(input, evaluator);
        if(Objects.nonNull(ans))
          System.out.println(ans);
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
        scanner -> evaluator.eval(
            SimpleParser.create(scanner)
                .parse()
        )
    );
  }

  public static Root parsing(String filenameOrCode, boolean is_file) throws FileNotFoundException {
    return GeneralParser
        .create(new GeneralScanner(filenameOrCode, is_file))
        .parse();
  }

  public static Object eval(String filenameOrCode, boolean is_file) {
    try {
      Root parsing = parsing(filenameOrCode, is_file);
      Root transformedIr = (Root) parsing.accept(new IrTransforms.Convert2OldIR(), null);
      return Evaluators.evaluator(true).eval(transformedIr);
    }catch (Exception e){
      e.printStackTrace();
    }
    return null;
  }

}

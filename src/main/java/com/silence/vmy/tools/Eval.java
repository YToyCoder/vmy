package com.silence.vmy.tools;

import com.silence.vmy.compiler.AST;
import com.silence.vmy.compiler.Scanners;
import com.silence.vmy.compiler.SimpleParser;
import com.silence.vmy.runtime.Evaluator;
import com.silence.vmy.runtime.Evaluators;

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
  public static void repl(){
    repl(Evaluators.variableStoreTreeEvaluator());
  }

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

}

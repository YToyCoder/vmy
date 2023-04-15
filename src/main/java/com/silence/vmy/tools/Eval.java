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
        Root transformedIr = (Root) parsing.accept(new IrTransforms.Convert2OldIR(), null);
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

  public static Root parsing(String filenameOrCode, boolean isFile) throws FileNotFoundException {
    return GeneralParser
            .create(new GeneralScanner(filenameOrCode, isFile))
            .parse();
  }

  public static Object eval(String filenameOrCode, boolean is_file) {
    try {
      Root parsing = parsing(filenameOrCode, is_file);
      Root transformedIr = (Root) parsing.accept(new IrTransforms.Convert2OldIR(), null);
      return Evaluators.evaluator(true).eval(transformedIr);
    }catch (Exception e){
      throw new RuntimeException(e);
    }
  }

}

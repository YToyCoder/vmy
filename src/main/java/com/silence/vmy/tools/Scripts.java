package com.silence.vmy.tools;

import com.silence.vmy.compiler.*;
import com.silence.vmy.runtime.Evaluator;
import com.silence.vmy.runtime.Evaluators;
import com.silence.vmy.runtime.VisitingEvaluator;

import java.io.*;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * vmy script support
 *
 * types of token need support :
 * 1. Literal : Int, Double, String, Char
 * 2. Comma : ,
 * 3. Black
 * 4. Declaration
 * 5. Identifier
 * 6. Operator
 */
public class Scripts {
  private Scripts() {}

  /**
   * run scripts
   * @param script_files files of script
   */
  public static void run(String[] script_files){
    for (String file_path : script_files)
      do_with_file_input_scanner(
          file_path,
          eval_with_scanner()
      );
  }

  public static void run(String[] script_files, String evaluator_type){
    Evaluator evaluator = switch (evaluator_type){
      case VisitingEvaluator -> new VisitingEvaluator();
      default -> Evaluators.evaluator(true);
    };
    for(String file_path : script_files)
      do_with_file_input_scanner(
          file_path,
          eval(evaluator)
      );
  }

  public static final String DefaultEvaluator   = "d";
  public static final String VisitingEvaluator  = "v";

  public static void do_with_file_input_scanner(
    String file, 
    Consumer<FileInputScanner> scanner_consumer
  ){

    try(FileInputScanner scanner = new FileInputScanner(file)) {
      scanner_consumer.accept(scanner);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

  }

  /**
   * get a function that input is scanner, evaluate scanner content
   * @return {@link Consumer }
   */
  public static Consumer<FileInputScanner> eval_with_scanner() {
    return eval(Evaluators.evaluator(true));
  }

  public static Consumer<FileInputScanner> eval(Evaluator evaluator){
    return scanner -> Objects.requireNonNull(evaluator).eval(
        SimpleParser.create(scanner).parse()
    );
  }

  /**
   * safe way to run with scanner
   * @param file_or_pure_string
   * @param is_file
   * @param run_with_scanner
   */
  public static Object run_with_file_input_scanner(
    String file_or_pure_string, 
    boolean is_file, 
    Function<FileInputScanner, Object> run_with_scanner
  ){

    try(FileInputScanner scanner = new FileInputScanner(file_or_pure_string, is_file)){
      return run_with_scanner.apply(scanner);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * default is file
   * @param file_or_pure_string
   * @param run_with_scanner
   */
  public static Object run_with_file_input_scanner(
    String file_or_pure_string, 
    Function<FileInputScanner, Object> run_with_scanner
  ){
    return run_with_file_input_scanner(file_or_pure_string, true, run_with_scanner);
  }

  public static FileInputScanner file_scanner(String file) throws FileNotFoundException {
    return new FileInputScanner(file);
  }

}

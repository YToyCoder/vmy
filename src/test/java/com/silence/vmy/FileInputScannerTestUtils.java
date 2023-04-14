package com.silence.vmy;

import com.silence.vmy.compiler.AST;
import com.silence.vmy.compiler.oldIR.FileInputScanner;
import com.silence.vmy.compiler.oldIR.Token;
import com.silence.vmy.runtime.VisitingEvaluator;
import com.silence.vmy.tools.Eval;
import com.silence.vmy.tools.Scripts;
import com.silence.vmy.tools.Utils;

import java.util.function.Consumer;

public class FileInputScannerTestUtils {
  private FileInputScannerTestUtils(){}

  public static void do_with_instance(String file, Consumer<FileInputScanner> scanner_consumer){
    Scripts.do_with_file_input_scanner(file, scanner_consumer);
    Eval.eval(file, true);
  }

  public static String ofScript(String _name){
    return String.format("%s/%s", Utils.get_dir_of_project("example" ), _name);
  }

  public static Consumer<FileInputScanner> build_with_scanner(){
    return scanner -> AST.build(scanner);
  }

  public static Consumer<FileInputScanner> eval_with_scanner() {
//    return scanner -> AST.evaluator(true).eval(AST.build(scanner));
    return Scripts.eval(new VisitingEvaluator());
  }

  public static Token let_token(){
    return declaration_token("let");
  }

  public static Token val_token(){
    return declaration_token("val");
  }

  private static Token declaration_token(String val){
    return new Token(Token.Declaration, val);
  }

  public static Token identifier_token(String val){
    return new Token(Token.Identifier, val);
  }

}

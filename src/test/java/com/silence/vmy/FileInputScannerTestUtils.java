package com.silence.vmy;

import com.silence.vmy.Utils;

import java.io.FileNotFoundException;
import java.util.function.Consumer;

public class FileInputScannerTestUtils {
  private FileInputScannerTestUtils(){}

  public static void do_with_instance(String file, Consumer<Scripts.FileInputScanner> scanner_consumer){
    Scripts.do_with_file_input_scanner(file, scanner_consumer);
  }

  public static String ofScript(String _name){
    return String.format("%s/%s", Utils.get_dir_of_project("scripts" ), _name);
  }

  public static Consumer<Scripts.FileInputScanner> build_with_scanner(){
    return scanner -> AST.build(scanner);
  }

  public static Consumer<Scripts.FileInputScanner> eval_with_scanner() {
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

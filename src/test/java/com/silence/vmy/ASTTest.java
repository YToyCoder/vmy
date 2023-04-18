package com.silence.vmy;

import static org.junit.Assert.assertThrows;

import com.silence.vmy.compiler.AST;
import com.silence.vmy.compiler.GeneralParser;
import com.silence.vmy.compiler.GeneralParserTest;
import com.silence.vmy.compiler.visitor.ASTProcessingException;
import com.silence.vmy.compiler.Scanners;
import com.silence.vmy.compiler.SimpleParser;
import com.silence.vmy.tools.Scripts;
import org.junit.Test;

import java.util.Set;
import java.util.function.Consumer;

public class ASTTest {

  @Test
  public void test1(){
    AST.build(Scanners.scan("1 + 2"));
  }

  @Test
  public void testBuildException(){
    assertThrows(ASTProcessingException.class, () -> AST.build(Scanners.scan("1 + 2)")));
    assertThrows(ASTProcessingException.class, () -> AST.build(Scanners.scan("1 +++ 2")));
    assertThrows(ASTProcessingException.class, () -> AST.build(Scanners.scan(" * 2")));
    assertThrows(ASTProcessingException.class, () -> AST.build(Scanners.scan("1 * ")));
    assertThrows(ASTProcessingException.class, () -> AST.build(Scanners.scan(" + 2")));
  }

  @Test
  public void testBuild(){
    AST.build(Scanners.scan("1 + 2 * (3 + 4)"));
    AST.build(Scanners.scan("1 + 2 / (3 + 4)"));
    AST.build(Scanners.scan("(1 + 2) / (3 + 4)"));
    AST.build(Scanners.scan("(1 + 2) / (3 + 4 * 5)"));
    AST.build(Scanners.scan("(1 + 2) / (3 + 4 * (5 + 1))"));
  }

  static Set<String> testCases = Set.of(
      "1 + 2 * ( 3 + 4 )",
      "1 + 2 / (3 + 4 )",
      "(1 + 2) / ( 3 + 4 ) * 4",
      "( 2 + 4 ) * 2 + 3 - 4 * 5"
  );
  @Test
  public  void testBuildWithScanner(){
    cases4(
        testCases,
        el -> AST.build(Scanners.scanner(el))
    );
  }

  @Test
  public void testDeclaration(){
    cases4(
        Set.of(
            "let a",
            "val b",
            "val c : Int"
        ),
        el -> AST.build(Scanners.scanner(el))
    );
  }

  @Test
  public void testAssignment(){
    cases4(
        Set.of(
            "let a = b",
            "let a : Int = 1",
            "let a : Int = 1 + 2",
            "let a : Int = 1 + 2 * ( 10 + 9) - 1"
        ),
        el -> AST.build(Scanners.scanner(el))
    );
  }

  void cases4(Set<String> cases, Consumer<String> test){ cases.forEach(test); }

  @Test
  public void string_literal_test(){
    cases4(
        Set.of(
            "\"literal string\"",
            "let a = \" literal string \""
        ),
        el -> AST.build(Scanners.scanner(el))
    );
  }

  @Test
  public void print_call_test(){
    cases4(
        Set.of(
            "print(1)",
            "print(1, 2)",
            "print(1 + 2, 3)",
            "print(1 + 4 * 5 , 7 - 10 * ( 3 -1 ))"
        ),
        el -> AST.build(Scanners.scanner(el))
    );
  }

  @Test
  public void bool_literal_test(){

    cases4(
        Set.of(
            "let b : Boolean = true "
        ),
        el -> AST.build(Scanners.scanner(el))
    );
  }

  @Test
  public void block_test(){

    cases4(
        Set.of(
            "{ let a = 1 }",
            "{ let b : Int = a }"
        ),
        el -> AST.build(Scanners.scanner(el))
    );
  }

  @Test
  public void while_loop_test(){

    cases4(
        Set.of(
            "while(true){ let a = 1 }",
            "while(a < 2){ let a = 1 }",
            "while(a < b + c * (1 - 5)) { a = 1 + ( 3 - 2 ) * 1.5}",
            "while(a + b < b + c * (1 - 5)) { a = 1 + ( 3 - 2 ) * 1.5}"
        ),
        el -> AST.build(Scanners.scanner(el))
    );
  }

  @Test
  public void number_literal_test(){
    FileInputScannerTestUtils.do_with_instance(
        FileInputScannerTestUtils.ofScript("number_literal_token_support_test.vmy"),
        scanner -> AST.build(scanner)
    );
  }

  @Test
  public void ast_script_test(){
    FileInputScannerTestUtils.do_with_instance(
        FileInputScannerTestUtils.ofScript("ast_test_for_script.vmy"),
        scanner -> AST.build(scanner)
    );
  }

  @Test
  public void script_block_test(){
    FileInputScannerTestUtils.do_with_instance(
        FileInputScannerTestUtils.ofScript("block_test.vmy"),
        FileInputScannerTestUtils.build_with_scanner()
    );
  }

  @Test
  public void script_negative_value_test() {
    FileInputScannerTestUtils.do_with_instance(
      FileInputScannerTestUtils.ofScript("negative_number_test.vmy"), 
      FileInputScannerTestUtils.build_with_scanner()
    );
  }

  @Test
  public void if_else_in_script_test() {
    FileInputScannerTestUtils.do_with_instance(
        FileInputScannerTestUtils.ofScript("if_else_token_test.vmy"),
        FileInputScannerTestUtils.build_with_scanner()
    );
  }

  @Test
  public void function_support_test() {
    Scripts.run_with_file_input_scanner(
      """
        fun a(param : Int) : Int {
          param = 1
        }
          """, 
      false,
      scanner ->
//        Scripts.eval(new VisitingEvaluator()).accept(scanner);
        SimpleParser.create(scanner).parse()
      );

    Scripts.run_with_file_input_scanner(
      """
        fun a(name: String, age : Int) : void {
          print(\"hello\")
        }
          """,
      false,
      scanner -> SimpleParser.create(scanner).parse()
      );

    GeneralParserTest.run_with_scanner_s(
      """
        fun func(name: String) : String {
          return "return"
        }
          """,
      scanner -> GeneralParser.create(scanner).parse()
    );
  }
}
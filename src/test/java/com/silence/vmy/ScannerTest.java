package com.silence.vmy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.logging.Logger;

import com.silence.vmy.compiler.*;
import com.silence.vmy.compiler.oldIR.FileInputScanner;
import com.silence.vmy.compiler.oldIR.Scanner;
import com.silence.vmy.compiler.oldIR.Token;
import com.silence.vmy.tools.Scripts;
import com.silence.vmy.tools.Utils;
import org.junit.Test;

public class ScannerTest {
  private static final Logger log = Logger.getLogger(ScannerTest.class.getName());

  static Map<Integer, String> oneCharSource = new HashMap<>();

  static {
    oneCharSource.put(Token.Identifier, "*");
    oneCharSource.put(Token.Identifier, "/");
    oneCharSource.put(Token.Identifier, "-");
    oneCharSource.put(Token.Identifier, "+");
    oneCharSource.put(Token.INT_V, "12");
    oneCharSource.put(Token.INT_V, " 12 ");
    oneCharSource.put(Token.INT_V, "140");
    oneCharSource.put(Token.DOUBLE_V, "1.2");
  }

  @Test
  public void testHandler(){
    oneCharSource.forEach((k, v) -> {
      List<Token> tokens = Scanners.scan(v);
      assertTrue(v + "length should be 1, but is " + tokens.size(), tokens.size() == 1);
      assertEquals(v, (long)tokens.get(0).tag, (long)k);
      assertEquals(v, tokens.get(0).value, v.trim());
    });

    assertThrows("? should not be handled" ,RuntimeException.class, () -> {
      Scanners.scan(",");
    });

    List<Token> tokens = Scanners.scan("1 2");
    assertTrue("1 2", tokens.size() == 2);
    tokens.forEach(el -> {
      assertEquals(Token.INT_V, el.tag);
    });

  }

  @Test
  public void testHandler2() {
    final String source = "1*2";
    List<Token> tokens2 = Scanners.scan(source);
    assertTrue(source, tokens2.size() == 3);
    assertTrue(source + " should be 1", tokens2.get(0).tag == Token.INT_V);
    assertEquals(source + " should be 1", tokens2.get(0).value, "1");

    assertTrue(source + " should be *", tokens2.get(1).tag == Token.Identifier);
    assertEquals(source + " should be *", tokens2.get(1).value, "*");

    assertTrue(source + " should be 1", tokens2.get(2).tag == Token.INT_V);
    assertEquals(source + " should be 1", tokens2.get(2).value, "2");
  }

  @Test
  public void testHandler3(){
    final String source = "1*2 + ( 3 - 1 ) ";
    List<Token> tokens = Scanners.scan(source);
    assertEqualTo(
      new Token[]{ 
        new Token(Token.INT_V, "1"), 
        new Token(Token.Identifier, "*"),
        new Token(Token.INT_V, "2"),
        new Token(Token.Identifier, "+"),
        new Token(Token.Identifier, "("),
        new Token(Token.INT_V, "3"),
        new Token(Token.Identifier, "-"),
        new Token(Token.INT_V, "1"),
        new Token(Token.Identifier, ")")
      }, 
      tokens.toArray(new Token[0]));
  }

  void assertEqualTo(Token[] expects, Token[] real){
    assertTrue("length should equal", expects.length == real.length);
    for(int i=0; i<expects.length; i++){
      assertTrue("token " + expects[i].value, expects[i].tag == real[i].tag);
      if(real[i].tag != Token.NewLine)
        assertEquals("token " + expects[i].value, expects[i].value , real[i].value);
    }
  }

  // test token like : a = b
  @Test
  public  void assignmentTest(){
    assertEqualTo(
        new Token[]{
            new Token(Token.Assignment,"=")
        },
        Scanners.scan(" = ").toArray(new Token[0])
    );
  }

  @Test
  public void declarationTest(){
    assertEqualTo(
        new Token[]{
            new Token(Token.Declaration, "let"),
            new Token(Token.Declaration, "val")
        },
        Scanners.scan("let val").toArray(new Token[0])
    );
  }

  @Test
  public void string_literal_test(){
    assertEqualTo(
        new Token[]{
            new Token(Token.Literal, "string literal"),
            new Token(Token.Literal, " has black ")
        },
        Scanners.scan("\"string literal\" \" has black \"").toArray(new Token[0])
    );
    assertThrows(
        LexicalException.class,
        () -> {
          Scanners.scan("\"");
        }
    );
    assertThrows(
        LexicalException.class,
        () -> {
          Scanners.scan("\" lexical\r\n \"");
        }
    );

  }

  @Test
  public void black_test(){
    assertEqualTo(
        new Token[]{
            new Token(Token.Declaration, Identifiers.VarDeclaration)
        },
        Scanners.scan("let     ").toArray(new Token[0])
    );
    Scanner scanner = Scanners.scanner("let  let ");
    while(scanner.hasNext()){
      scanner.next();
    }
  }

  @Test
  public void print_call() {
    assertEqualTo(
        new Token[]{
            new Token(Token.BuiltinCall, "print")
        },
        Scanners.scan("print").toArray(new Token[0])
    );

    assertEqualTo(
        new Token[]{
            new Token(Token.BuiltinCall, "print"),
            new Token(Token.INT_V, "1")
        },
        Scanners.scan("print 1").toArray(new Token[0])
    );
    assertEqualTo(
        new Token[]{
            new Token(Token.BuiltinCall, "print"),
            new Token(Token.Identifier, "("),
            new Token(Token.INT_V, "1"),
            new Token(Token.Comma, ","),
            new Token(Token.INT_V, "2"),
            new Token(Token.Identifier, ")")
        },
        Scanners.scan("print(1, 2)").toArray(new Token[0])
    );
  }

  @Test
  public void while_test(){

    assertEqualTo(
        new Token[]{
            new Token(Token.Builtin, "while")
        },
        Scanners.scan("while").toArray(new Token[0])
    );

    assertEqualTo(
        new Token[]{
            new Token(Token.Builtin, "while"),
            new Token(Token.Identifier, "("),
            new Token(Token.Identifier, ")")
        },
        Scanners.scan("while()").toArray(new Token[0])
    );
  }

  @Test
  public void braces_test(){

    assertEqualTo(
        new Token[]{
            new Token(Token.Identifier, "{"),
            new Token(Token.Identifier, "}")
        },
        Scanners.scan("{}").toArray(new Token[0])
    );

    assertEqualTo(
        new Token[]{
            new Token(Token.Builtin, "while"),
            new Token(Token.Identifier, "("),
            new Token(Token.Identifier, ")"),
            new Token(Token.Identifier, "{"),
            new Token(Token.Identifier, "}")
        },
        Scanners.scan("while(){}").toArray(new Token[0])
    );
  }

  @Test
  public void bool_literal_test(){

    assertEqualTo(
        new Token[]{
            new Token(Token.Literal, "true"),
            new Token(Token.Literal, "false")
        },
        Scanners.scan("true false").toArray(new Token[0])
    );
  }

  private static String ofScript(String name){
    return String.format("%s/%s", Utils.get_dir_of_project("scripts"), name);
  }
  @Test
  public void file_scanner_test(){
    log.info(Utils.project_dir);
    FileInputScanner scanner = null;
    try {
      scanner = Scripts.file_scanner(ofScript("hello_word.vmy"));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    System.out.println(scanner.scan(""));
    try {
      scanner.close();
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Test
  public void file_scanner_toke_support_test(){
    FileInputScannerTestUtils.do_with_instance(
        ofScript("token_support_test.vmy"),
        el -> {
          assertEqualTo(
              new Token[]{
                  new Token(Token.Declaration, "let"),
                  new Token(Token.Identifier, "a"),
                  new Token(Token.Assignment, "="),
                  new Token(Token.Literal, "1"),
                  new Token(Token.NewLine, ""),
                  new Token(Token.Builtin, "while"),
                  new Token(Token.Identifier, Identifiers.OpenParenthesis),
                  new Token(Token.Literal, "1"),
                  new Token(Token.Identifier, Identifiers.ClosingParenthesis),
                  new Token(Token.Identifier, Identifiers.OpenBrace),
                  new Token(Token.NewLine, ""),
                  new Token(Token.BuiltinCall, Identifiers.Print),
                  new Token(Token.Identifier, Identifiers.OpenParenthesis),
                  new Token(Token.Literal, "\"hello, word\""),
                  new Token(Token.Identifier, Identifiers.ClosingParenthesis),
                  new Token(Token.NewLine, ""),
                  new Token(Token.Identifier, Identifiers.ClosingBrace)
              },
              to_token_arr(el.scan(""))
          );
        }
    );
  }

  @Test
  public void number_literal_token_support_test(){
    FileInputScannerTestUtils.do_with_instance(
        ofScript("number_literal_token_support_test.vmy"),
        scanner -> {
          assertEqualTo(
              new Token[] {
                  new Token(Token.Declaration, "let"),
                  new Token(Token.Identifier, "a"),
                  new Token(Token.Identifier, ":"),
                  new Token(Token.Identifier, "Int"),
                  new Token(Token.Assignment, "="),
                  new Token(Token.Literal, "1"),
                  new Token(Token.NewLine, ""),
                  new Token(Token.Declaration, "let"),
                  new Token(Token.Identifier, "b"),
                  new Token(Token.Identifier, ":"),
                  new Token(Token.Identifier, "Boolean"),
                  new Token(Token.Assignment, "="),
                  new Token(Token.Literal, "true"),
              },
              to_token_arr(scanner.scan(""))
          );
        }
    );
  }

  @Test
  public void annotation_support_test(){
    FileInputScannerTestUtils.do_with_instance(
        ofScript("annotation_test.vmy"),
        scanner -> {
          assertEqualTo(
              new Token[]{
                  new Token(Token.NewLine, ""),
                  new Token(Token.Declaration, "let"),
                  new Token(Token.Identifier, "a"),
                  new Token(Token.Identifier, ":"),
                  new Token(Token.Identifier, "Int"),
                  new Token(Token.Assignment, "="),
                  new Token(Token.Literal, "1")
              },
              to_token_arr(scanner.scan(""))
          );
        }
    );
  }

  @Test
  public void if_else_token_support_test(){
    FileInputScannerTestUtils.do_with_instance(
        ofScript("if_else_token_test.vmy"),
        scanner ->
          assertEqualTo(
              new Token[]{
                  new Token(Token.NewLine, ""),
                  new Token(Token.Builtin, "if"),
                  new Token(Token.Identifier, Identifiers.OpenParenthesis),
                  new Token(Token.Literal, "true"),
                  new Token(Token.Identifier, Identifiers.ClosingParenthesis),
                  new Token(Token.Identifier, Identifiers.OpenBrace),
                  new Token(Token.NewLine, ""),
                  new Token(Token.BuiltinCall, "print"),
                  new Token(Token.Identifier, Identifiers.OpenParenthesis),
                  new Token(Token.Literal, "\"true\""),
                  new Token(Token.Identifier, Identifiers.ClosingParenthesis),
                  new Token(Token.NewLine, ""),
                  new Token(Token.Identifier, Identifiers.ClosingBrace),

                  new Token(Token.Builtin, "elif"),
                  new Token(Token.Identifier, Identifiers.OpenParenthesis),
                  new Token(Token.Literal, "false"),
                  new Token(Token.Identifier, Identifiers.ClosingParenthesis),
                  new Token(Token.Identifier, Identifiers.OpenBrace),
                  new Token(Token.NewLine, ""),
                  new Token(Token.BuiltinCall, "print"),
                  new Token(Token.Identifier, Identifiers.OpenParenthesis),
                  new Token(Token.Literal, "\"else if\""),
                  new Token(Token.Identifier, Identifiers.ClosingParenthesis),
                  new Token(Token.NewLine, ""),
                  new Token(Token.Identifier, Identifiers.ClosingBrace),

                  new Token(Token.Builtin, Identifiers.Else),
                  new Token(Token.Identifier, Identifiers.OpenBrace),
                  new Token(Token.NewLine, ""),
                  new Token(Token.BuiltinCall, "print"),
                  new Token(Token.Identifier, Identifiers.OpenParenthesis),
                  new Token(Token.Literal, "\"else\""),
                  new Token(Token.Identifier, Identifiers.ClosingParenthesis),
                  new Token(Token.NewLine, ""),
                  new Token(Token.Identifier, Identifiers.ClosingBrace),
              },
              to_token_arr(scanner.scan(""))
          )
    );
  }

  @Test
  public void quote_test(){
    System.out.println((int)'"');
  }

  private static Token[] to_token_arr(List<Token> tokens){
    return tokens.toArray(new Token[0]);
  }

  @Test
  public void function_literal_test() {
    Scripts.run_with_file_input_scanner(
        """
            function a(param : Int) : Int {
            }
            """,
        false,
        scanner -> {
            assertEqualTo(
                new Token[]{
                    new Token(Token.Identifier, "function"),
                    new Token(Token.Identifier, "a"),
                    new Token(Token.Identifier, Identifiers.OpenParenthesis),
                    new Token(Token.Identifier, "param"),
                    new Token(Token.Identifier, ":"),
                    new Token(Token.Identifier, "Int"),
                    new Token(Token.Identifier, Identifiers.ClosingParenthesis),
                    new Token(Token.Identifier, ":"),
                    new Token(Token.Identifier, "Int"),
                    new Token(Token.Identifier, "{"),
                    new Token(Token.NewLine, "\n"),
                    new Token(Token.Identifier, "}"),
                    new Token(Token.NewLine, "\n")
                },
                to_token_arr(scanner.scan(""))
            );
            return null;
        }
    );
  }

  @Test
  public void function_declaration_test() {
    Scripts.run_with_file_input_scanner(
        """
            function a(param : Int) : Int {
              let m : Int = 1
              return m + param
            }
            """,
        false,
        scanner -> {
            assertEqualTo(
                new Token[]{
                    new Token(Token.Identifier, "function"),
                    new Token(Token.Identifier, "a"),
                    new Token(Token.Identifier, Identifiers.OpenParenthesis),
                    new Token(Token.Identifier, "param"),
                    new Token(Token.Identifier, ":"),
                    new Token(Token.Identifier, "Int"),
                    new Token(Token.Identifier, Identifiers.ClosingParenthesis),
                    new Token(Token.Identifier, ":"),
                    new Token(Token.Identifier, "Int"),
                    new Token(Token.Identifier, "{"),
                    new Token(Token.NewLine, "\n"),
                    FileInputScannerTestUtils.let_token(),
                    FileInputScannerTestUtils.identifier_token("m"),
                    FileInputScannerTestUtils.identifier_token(":"),
                    FileInputScannerTestUtils.identifier_token("Int"),
                    new Token(Token.Assignment, "="),
                    new Token(Token.Literal, "1"),
                    new Token(Token.NewLine, "\n"),
                    FileInputScannerTestUtils.identifier_token("return"),
                    FileInputScannerTestUtils.identifier_token("m"),
                    FileInputScannerTestUtils.identifier_token("+"),
                    FileInputScannerTestUtils.identifier_token("param"),
                    new Token(Token.NewLine, "\n"),
                    new Token(Token.Identifier, "}"),
                    new Token(Token.NewLine, "\n")
                },
                to_token_arr(scanner.scan(""))
            );
            return null;
        }
    );
  }

  @Test
  public void printTokStartPosition(){
    Scripts.run_with_file_input_scanner(
      ofScript("if_else_eval_test.vmy"),
      scanner -> {
        while(scanner.hasNext()){
          var tok = scanner.next();
          System.out.println(tok);
        }
        return null;
      });
  }

}

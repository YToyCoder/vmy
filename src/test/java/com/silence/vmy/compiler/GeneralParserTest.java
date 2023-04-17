package com.silence.vmy.compiler;

import com.silence.vmy.compiler.tree.Root;
import com.silence.vmy.tools.Eval;
import com.silence.vmy.tools.Utils;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.function.Consumer;
import java.util.function.Function;

// test for GeneralParser
public class GeneralParserTest {

  public static void run_with_scanner_s(String content, Consumer<GeneralScanner> run){
    try{
      GeneralScanner scanner = new GeneralScanner(content,false);
      run.accept(scanner);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static void evalScript(
      String script,
      Function<GeneralScanner, Root> parsing,
      Consumer<Root> evalRoot
  ) {
    try {
      GeneralScanner scanner = new GeneralScanner(Utils.ofScript(script), true);
      Root ast = parsing.apply(scanner);;
      evalRoot.accept(ast);
    }catch(Exception e){
      throw new RuntimeException(e);
    }
  }

  public static void run_of_script(String script, Consumer<GeneralScanner> run){
    try {
      GeneralScanner scanner = new GeneralScanner(Utils.ofScript(script), true);
      run.accept(scanner);
    } catch (FileNotFoundException e){
      throw new RuntimeException(e);
    }
  }
  private Function<GeneralScanner,Root> doParsing() {
    return scanner -> GeneralParser.create(scanner).parse();
  }

  Consumer<GeneralScanner> parsing(){
    return scanner -> {
      var r = GeneralParser.create(scanner).parse();
      System.out.println("parsing r:" + r);
    };
  }

  @Test
  public void t1(){
    run_with_scanner_s(
        """
        val a = "hello" 
          a = "b"
        b = a = "gr"
        k = 1 + 2 + ( 2 * 1 - 3) / 1.0
        let m = m * k
        c = ---4
        """,
        scanner -> GeneralParser.create(scanner).parse());
  }

  @Test
  public void func(){
    run_with_scanner_s(
        """
        fun name() {
          let a = "hello"
          let b = 100
          val c = a + b
          return c
        }
        fun he(a : String) : Unit {
        }
        """,
        scanner -> GeneralParser.create(scanner).parse());
  }

  @Test
  public void type_decl(){
    run_with_scanner_s(
        """
            val c : String = "hello"
            """,
        scanner -> GeneralParser.create(scanner).parse()
    );
  }

  @Test
  public void ops(){
    run_with_scanner_s(
        """
            let a : Int = 10
            a += 12
            a -= 1
            a *= -1
            a /= 1.0
            """,
        parsing()
    );
  }

  @Test
  public void call_expr() {
    run_with_scanner_s(
        """
            call(a,b, 1.0)
            """,
        parsing()
    );
  }

  @Test
  public void comparingExpression(){
    run_with_scanner_s(
      """
        1 < 2
        2 * 4 - 3 <= 7 * variable
          """, 
      parsing());
  }

  @Test
  public void script(){
    run_of_script("general_parser.vmy", parsing());
    run_of_script("hello_word.vmy", parsing());
    evalScript("general_parser.vmy", doParsing() , Eval::evalRoot);
  }

}

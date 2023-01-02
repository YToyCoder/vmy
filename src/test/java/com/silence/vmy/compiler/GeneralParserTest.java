package com.silence.vmy.compiler;

import org.junit.Test;

import java.io.FileNotFoundException;
import java.util.function.Consumer;

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
}

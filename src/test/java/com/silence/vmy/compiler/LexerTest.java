package com.silence.vmy.compiler;

import com.silence.vmy.FileInputScannerTestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;

public class LexerTest {

  @Test
  public void test1() throws FileNotFoundException {
    GeneralScanner scanner = new GeneralScanner("hello,world", false);
    Assert.assertEquals(scanner.next(), new Tokens.Token(Tokens.TokenKind.Id,0,5, "hello")); // hello
    Assert.assertEquals(scanner.next(),new Tokens.Token(Tokens.TokenKind.Comma,5,6)); // ,
    Assert.assertEquals(scanner.peek(),new Tokens.Token(Tokens.TokenKind.Id,6,11, "world"));
    Assert.assertEquals(scanner.next(),new Tokens.Token(Tokens.TokenKind.Id,6,11, "world"));
    Assert.assertEquals(scanner.next(), new Tokens.Token(Tokens.TokenKind.newline, 11, 12));
    Assert.assertTrue("scanner is empty", !scanner.hasNext());
  }

  @Test
  public void test2() throws FileNotFoundException {
    GeneralScanner generalScanner = new GeneralScanner(FileInputScannerTestUtils.ofScript("while_loop_test.vmy"), true);
    while (generalScanner.hasNext()){
      Tokens.Token next = generalScanner.next();
      System.out.println(next);
    }
  }

}

package com.silence.vmy.compiler;

import org.junit.Assert;
import org.junit.Test;

public class CharReaderTests {

  @Test
  public void fromStringTest(){
    iterateAndCompare("abcdefg");
    CharReader charReader = CharReaders.fromString("hello,world\n");
    Assert.assertEquals(charReader.peek().c(),'h');
    Assert.assertEquals(charReader.next().c(),'h');
    Assert.assertEquals(charReader.peek().c(), 'e');
  }

  void iterateAndCompare(String source){
    CharReader charReader = CharReaders.fromString(source);
    int i=0;
    for(char c : source.toCharArray()){
      CharReaders.CharInFile next = charReader.next();
      Assert.assertEquals(c, next.c());
      Assert.assertEquals(i++, next.col());
    }
  }
}

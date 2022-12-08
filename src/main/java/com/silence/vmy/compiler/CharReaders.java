package com.silence.vmy.compiler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Objects;

public class CharReaders {

  public record CharInFile(char c, int row, int col) {
    boolean isDigital(){
      return Character.isDigit(c);
    }

    boolean isAlphabetic(){
      return Character.isAlphabetic(c);
    }

    long location(){
      return Tokens.location(row, col);
    }

    boolean charIs(char literal){
      return c() == literal;
    }
  }

  public static CharReader fromString(String source){
    return new FromStringCharReader(source);
  }

  public static CharReader fromFile(String name) throws FileNotFoundException {
    return new FromFileCharReader(name);
  }

  private static class FromStringCharReader implements CharReader{
    private final String source;
    private int position = -1;
    private final char[] charArray;
    private CharInFile cache;
    private final int rowNum;

    public FromStringCharReader(String source, int _rowNumber) {
      this.source = source;
      charArray = source.toCharArray();
      this.rowNum = _rowNumber;
    }

    public FromStringCharReader(String source){
      this(source, 0);
    }

    @Override
    public CharInFile peek() {
      check();
      return cache;
    }

    void check(){
      if(empty())
        throw new RuntimeException("content is empty");
      if(Objects.isNull(cache)){
        position++;
        cache =  new CharInFile( position == charArray.length ?  '\n' : charArray[position], rowNum, position);
      }
    }

    @Override
    public CharInFile next() {
      check();
      CharInFile res = cache;
      cache = null;
      return res;
    }

    @Override
    public boolean empty() {
      return position >= charArray.length && Objects.isNull(cache);
    }

    @Override
    public void close() {
    }
  }

  private static class FromFileCharReader implements CharReader {
    private final String name;
    private final RandomAccessFile file;
    private int current_row = 0;
    private FromStringCharReader lineReader;

    private FromFileCharReader(String name) throws FileNotFoundException {
      this.name = name;
      file = new RandomAccessFile(name, "rw");
    }

    private void checkAndSet(){
      if(Objects.isNull(lineReader) || lineReader.empty()){
        // read
        String line = null;
        try {
          line = file.readLine();
        }catch (IOException e){
          e.printStackTrace();
        }
        if(Objects.nonNull(line)){
          lineReader =new FromStringCharReader(line, current_row++);
        }
      }
    }

    @Override
    public CharInFile peek() {
      checkAndSet();
      return lineReader.peek();
    }

    @Override
    public CharInFile next() {
      checkAndSet();
      return lineReader.next();
    }

    @Override
    public boolean empty() {
      checkAndSet();
      return Objects.isNull(lineReader) || lineReader.empty();
    }

    @Override
    public void close() throws IOException {
      file.close();
    }
  }
}

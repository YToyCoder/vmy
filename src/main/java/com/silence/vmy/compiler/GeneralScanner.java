package com.silence.vmy.compiler;

import java.io.FileNotFoundException;
import java.util.Objects;
import java.util.function.Function;

import com.silence.vmy.compiler.Tokens.TokenKind;

public class GeneralScanner implements Lexer{
  private CharReader charReader;
  private final String fileName;
  protected Tokens.Token tok;
  protected Tokens.Token pre;

  public GeneralScanner(String fileNameOrContent, boolean is_file) throws FileNotFoundException {
    this.fileName = fileNameOrContent;
    charReader = is_file ? CharReaders.fromFile(fileNameOrContent) : CharReaders.fromString(fileNameOrContent);
  }

  @Override
  public Tokens.Token next() {
    if(Objects.isNull(tok)){
      pre = fetchToken();
    }else{
      pre = tok;
      tok = null;
    }
    return pre;
  }

  protected Tokens.Token fetchToken() {
    if(hasChar()){
      skipBlank();
      CharReaders.CharInFile peekChar = peekChar();
      return switch (peekChar.c()){
        case '"' -> handle_string_literal();
        case '#' -> handle_annotation();
        case '1', '2','3', '4', '5',
             '6', '7','8', '9', '0'-> handle_digit_literal();
        case ',' -> one_char(Tokens.TokenKind.Comma);
        case '.' -> one_char(Tokens.TokenKind.Dot);
        case '(' -> one_char(Tokens.TokenKind.LParenthesis);
        case ')' -> one_char(Tokens.TokenKind.RParenthesis);
        case '{' -> one_char(Tokens.TokenKind.LBrace);
        case '}' -> one_char(Tokens.TokenKind.RBrace);
        case ':' -> one_char(Tokens.TokenKind.Colon);
        case '[' -> one_char(TokenKind.ArrOpen);
        case ']' -> one_char(TokenKind.ArrClose);
        case '>' -> {
          CharReaders.CharInFile startChar = nextChar();
          if(hasChar() && peekChar().charIs('='))
            yield createTok(Tokens.TokenKind.Ge, startChar.location(), nextChar().location() + 1);
          yield  createTok(Tokens.TokenKind.Greater, startChar.location(), startChar.location() + 1);
        }
        case '<' -> {
          CharReaders.CharInFile startChar = nextChar();
          if(hasChar() && peekChar().charIs('=')){
            yield createTok(Tokens.TokenKind.Le, startChar.location(), nextChar().location() + 1);
          }
          yield createTok(Tokens.TokenKind.Less, startChar.location(), startChar.location() + 1);
        }
        case '=' -> {
          CharReaders.CharInFile startChar = nextChar();
          if(hasChar() && peekChar().charIs('=')){
            yield createTok(Tokens.TokenKind.Equal, startChar.location(), nextChar().location() + 1);
          };
          yield createTok(Tokens.TokenKind.Assignment,startChar.location(),startChar.location() + 1);
        }
        case '!' -> {
          CharReaders.CharInFile startChar = nextChar();
          if(!hasChar() || !peekChar().charIs('=')){
            throw new LexicalException("! should follow with =, at row %d, col %d".formatted(peekChar.row(), peekChar.col()));
          }
          yield createTok(Tokens.TokenKind.NotEqual, startChar.location(), nextChar().location() + 1);
        }
        case '+' -> {
          CharReaders.CharInFile startChar = nextChar();
          if(hasChar()){
            if(peekChar().charIs('=')){
              yield createTok(Tokens.TokenKind.AddEqual, startChar.location(), nextChar().location() + 1);
            }else if(peekChar().charIs('+'))
              yield createTok(Tokens.TokenKind.Concat, startChar.location(), nextChar().location() + 1);
          }
          yield createTok(Tokens.TokenKind.Add, startChar.location(), startChar.location() + 1);
        }
        case '-' -> {
          CharReaders.CharInFile startChar = nextChar();
          if(hasChar() && peekChar().charIs('=')){
            yield createTok(Tokens.TokenKind.SubEqual, startChar.location(), nextChar().location() + 1);
          }
          yield createTok(Tokens.TokenKind.Sub, startChar.location(), startChar.location() + 1);
        }
        case '*' -> {
          CharReaders.CharInFile startChar = nextChar();
          if(hasChar() && peekChar().charIs('=')){
            yield createTok(Tokens.TokenKind.MultiEqual, startChar.location(), nextChar().location() + 1);
          }
          yield createTok(Tokens.TokenKind.Multi, startChar.location(), startChar.location() + 1);
        }
        case '/' -> {
          CharReaders.CharInFile startChar = nextChar();
          if(hasChar() && peekChar().charIs('=')){
            yield createTok(Tokens.TokenKind.DivEqual, startChar.location(), nextChar().location() + 1);
          }
          yield createTok(Tokens.TokenKind.Div, startChar.location(), startChar.location() + 1);
        }
        case '\n','\r' -> {
          CharReaders.CharInFile startChar = nextChar();
          if(hasChar() && peekChar().charIs('\r'))
            System.out.printf("newline char is %d%n", (int)nextChar().c());
          yield createTok(Tokens.TokenKind.newline, startChar.location(), peekChar.location() + 1);
        }
        // ignore token start with blank char
        case ' ' -> { nextChar(); yield fetchToken();}
        // TODO: 2022/12/
        default -> {
          if(peekChar.isAlphabetic()){
            yield handle_alphabetic_start();
          }
          throw new IllegalStateException("Unexpected value: %c/%d, row: %d, col: %d".formatted(
              peekChar.c(),
              (int)peekChar.c(),
              peekChar.row(),
              peekChar.col()));
        }
      };
    }
    throw new LexicalException("content is empty");
  }

  protected void skipBlank(){
    while (hasChar() && peekChar().charIs(' '))
      nextChar();
  }

  protected Tokens.Token createTok(Tokens.TokenKind kind, long start, long end){
    return new Tokens.Token(kind, start, end);
  }

  protected Tokens.Token createPayloadTok(Tokens.TokenKind kind, long start, long end, String name){
    return new Tokens.Token(kind, start,end, name);
  }

  // , . ( ) { }
  protected Tokens.Token one_char(Tokens.TokenKind kind){
    CharReaders.CharInFile charInFile = nextChar();
    return createTok(kind, charInFile.location(), charInFile.location() + 1);
  }

  // 字母开头
  protected Tokens.Token handle_alphabetic_start(){
    // ID Fun Val Let
    // If Elif Else While For
    // return
    StringBuilder builder = new StringBuilder();
    CharReaders.CharInFile startChar = peekChar();
    // build till not alphabetic
    while (hasChar() && peekChar().isAlphabetic()){
      builder.append(nextChar().c());
    }
    // case _ , like name_age
    if(!hasChar() || !peekChar().charIs('_')){
      final String string = builder.toString();
      Function<Tokens.TokenKind, Tokens.Token> createToken =
          kind -> createTok(kind, startChar.location(), startChar.location() + builder.length());
      return switch (string){
        case "fun" -> createToken.apply(Tokens.TokenKind.Fun);
        case "val" -> createToken.apply(Tokens.TokenKind.Val);
        case "let" -> createToken.apply(Tokens.TokenKind.Let);
        case "if" -> createToken.apply(Tokens.TokenKind.If);
        case "elif" -> createToken.apply(Tokens.TokenKind.Elif);
        case "else" -> createToken.apply(Tokens.TokenKind.Else);
        case "while" -> createToken.apply(Tokens.TokenKind.While);
        case "for" -> createToken.apply(Tokens.TokenKind.For);
        case "return" -> createToken.apply(Tokens.TokenKind.Return);
        case "true"  -> createToken.apply(Tokens.TokenKind.True);
        case "false"  -> createToken.apply(Tokens.TokenKind.False);
        case "in" -> createToken.apply(Tokens.TokenKind.In);
        default -> createPayloadTok(
            Tokens.TokenKind.Id,
            startChar.location(),
            startChar.location() + builder.length(),
            builder.toString()); // identifier
      };
    }

    CharReaders.CharInFile tempStore = null;
    while(hasChar() && ((tempStore = peekChar()).isAlphabetic() || tempStore.charIs('_'))){
      builder.append(nextChar().c());
    }
    return createPayloadTok(
        Tokens.TokenKind.Id,
        startChar.location(),
        startChar.location() + builder.length(),
        builder.toString());
  }

  // between "..."
  protected Tokens.Token handle_string_literal(){
    CharReaders.CharInFile startChar = nextChar();
    StringBuilder builder = new StringBuilder();
    while (hasChar() && !charIs(peekChar(), '"')){
      builder.append(nextChar().c());
    }
    if(!hasChar() || !charIs(peekChar(), '"')){
      throw new LexicalException("string literal has no closing quote!!!");
    }
    CharReaders.CharInFile endChar = nextChar();
    return createPayloadTok(
        Tokens.TokenKind.StringLiteral,
        Tokens.location(startChar.row(), startChar.col()),
        Tokens.location(endChar.row(), endChar.col()),
        builder.toString());
  }


  // start with #
  protected Tokens.Token handle_annotation(){
    CharReaders.CharInFile startChar = nextChar();
    while (hasChar() && !charIs(peekChar(), '\n')){
      nextChar();
    }
    if(hasChar()){
      nextChar(); // drop '\n'
    }
    return createTok(
        Tokens.TokenKind.Annotation,
        startChar.location(),
        0);
  }

  // double or int
  protected Tokens.Token handle_digit_literal(){
    CharReaders.CharInFile startChar = nextChar();
    final StringBuilder builder = new StringBuilder().append(startChar.c());
    while(hasChar() && peekChar().isDigital()){
      builder.append(nextChar().c());
    }
    CharReaders.CharInFile endChar;
    if(!charIs(endChar = peekChar(), '.'))
      return createPayloadTok(
          Tokens.TokenKind.IntLiteral,
          Tokens.location(startChar.row(), startChar.col()),
          Tokens.location(endChar.row(), endChar.col()),
          builder.toString());
    // double
    builder.append(nextChar().c());
    while (hasChar() && (endChar = peekChar()).isDigital())
      builder.append(nextChar().c());
    return createPayloadTok(
        Tokens.TokenKind.DoubleLiteral,
        Tokens.location(startChar.row(), startChar.col()),
        Tokens.location(endChar.row(), endChar.col() + 1),
        builder.toString());
  }

  boolean hasChar(){ return !charReader.empty(); }
  private CharReaders.CharInFile peekChar() { return charReader.peek(); }
  private CharReaders.CharInFile nextChar() { return charReader.next(); }
  protected boolean charIs(CharReaders.CharInFile c, char literal){ return c.c() == literal; }
  @Override
  public Tokens.Token last() { return pre; }

  @Override
  public boolean hasNext() {
    if(Objects.nonNull(tok))
      return true;
    skipBlank();
    return hasChar();
  }

  @Override
  public Tokens.Token peek() {
    if(Objects.nonNull(tok))
      return tok;
    return tok = fetchToken();
  }

}

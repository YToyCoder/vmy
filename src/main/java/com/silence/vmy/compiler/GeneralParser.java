package com.silence.vmy.compiler;

import com.silence.vmy.compiler.tree.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Predicate;

public class GeneralParser implements Parser{
  private Lexer lexer;
  private Stack<BaseTree> nodes;
  private Tokens.Token token;
  private Tokens.Token pre;
  private List<Tokens.Token> savedTokens = new LinkedList<>();
  @Override
  public Root parse() {
    return null;
  }

  /**
   * e_fun = fun identifier expr "{" e_block "}"
   */
  private FunctionDecl compileFunc(){
    return null;
  }

  /**
   * expr = "(" ")" | "(" expr2 ")"
   */
  private Expression expr(){
    return null;
  }

  protected Tokens.Token next(){
    pre = token;
    return (token = savedTokens.isEmpty() ? lexer.next() : savedTokens.remove(0));
  }

  protected Tokens.Token token(){
    return token(0);
  }

  protected Tokens.Token token(int lookahead) {
    if(lookahead == 0) {
      return token;
    }else {
      ensureLookahead(lookahead);
      return savedTokens.get(lookahead - 1);
    }
  }

  protected void ensureLookahead(int lookahead) {
    for(int i= savedTokens.size(); i < lookahead && lexer.hasNext() ; i++)
      savedTokens.add(lexer.next());
  }

  protected boolean hasTok(){
    return Objects.nonNull(token) || !savedTokens.isEmpty() || lexer.hasNext();
  }

  boolean peekTok(Predicate<Tokens.TokenKind> tk){
    return hasTok() && tk.test(token().kind());
  }

  /**
   * expr2 =  expr3 | expr2 "," expr3
   */
  private List<Exception> expr2(){
    return null;
  }

  /**
   * e_block = [ expression ]
   */
  private BlockStatement compileBlock(){
    return null;
  }

  /**
   * expr3 = identifier "=" expr4
   */
  private Expression expr3(){
    return null;
  }

  /**
   * expression = varDecl "=" expr4
   *            | expr3
   */
  private Expression expression(){
    return null;
  }

  /**
   * one = identifier | literal | "(" multi ")" | call
   */
  Expression one(){
    Tokens.Token peek = token();
    return switch (peek.kind()){
      case IntLiteral,
          StringLiteral,
          CharLiteral -> literal();

      case LBrace -> {
        next();
        Expression ret = multi();
        // todo check
        next(); // ")"
        yield  ret;
      }
      // todo call
      default -> null; // error
    };
  }

  /**
   * unary = one | "+" unary | "-" unary
   */
  Expression unary(){
    if (peekTok(
        tokenKind -> switch (tokenKind){
          case Add, Sub -> true;
          default -> false;
        }
    )){
      Tokens.Token pre = next();
      Expression unary = unary();
      return new Unary(op2tag(pre.payload()), unary);
    }
    return one();
  }

  /**
   * literal = "true" | "false"
   *         | numberLiteral
   *         | stringLiteral
   *         | charLiteral
   *         | functionLiteral
   */
  Expression literal(){
    return null;
  }

  /**
   * add = multi
   *    | multi "+" multi
   *    | multi "-" multi
   */
  Expression add(){
    Expression left = multi();
    while(peekTok(
        tokenKind -> switch (tokenKind){
          case Add, Sub -> true;
          default -> false;
        }
    )){
      Tokens.Token op = next();
      Expression right = multi();
      left = (Expression) new BinaryOperateExpression(left, right, op2tag(op.payload()))
          .setPos(op.start());
    }
    return left;
  }

  /**
   * multi = unary | multi "*" unary | multi "/" one
   */
  Expression multi(){
    Expression left = unary();
    while(peekTok(
        kind -> switch (kind){
          case Multi, Div -> true;
          default -> false;
        })
    ){
      Tokens.Token op = next();
      Expression right = unary();
      left = (Expression) new BinaryOperateExpression(left, right, op2tag(op.payload()))
          .setPos(op.start());
    }
    return left;
  }

  BaseTree.Tag op2tag(String op){
    return switch (op){
      case "*" -> BaseTree.Tag.Multi;
      case "/" -> BaseTree.Tag.Div;
      default -> null;
    };
  }

  /**
   * call = identifier expr
   */
  private Expression call(){
    return null;
  }

}

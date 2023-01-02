package com.silence.vmy.compiler;

import com.silence.vmy.compiler.Tokens.TokenKind;
import com.silence.vmy.compiler.tree.*;
import com.silence.vmy.compiler.tree.Tree.Tag;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class GeneralParser implements Parser{
  private Lexer lexer;
  private Tokens.Token token;
  private Tokens.Token pre;
  private List<Tokens.Token> savedTokens = new LinkedList<>();

  GeneralParser(Lexer _lexer){
    this.lexer = _lexer;
    next(); // fill token
  }

  public static Parser create(Lexer lexer){
    return new GeneralParser(lexer);
  }

  @Override
  public Root parse() {
    return Trees.createCompileUnit(compileBlock(TokenKind.EOF));
  }

  /**
   * e_fun = fun identifier expr "{" e_block "}"
   */
  private FunctionDecl compileFunc(){
    if(peekTok(tk -> tk != TokenKind.Fun))
      error(next());
    Tokens.Token decl = next();
    String name = null;
    if(peekTok(tk -> tk == TokenKind.Id))
      name = next().payload();
    return new FunctionDecl(
        name,
        ((ListExpr) expr()).body(),
        compileBlock(TokenKind.LParenthesis, TokenKind.RParenthesis),
        decl.start()
    );
  }

  /**
   * expr = "(" ")" | "(" expr2 ")"
   */
  private Expression expr(){
    if(token().kind() != TokenKind.LParenthesis){
      throw new LexicalException("expression error position %d".formatted(token().start()));
    }

    Tokens.Token start = next();
    if(peekTok(kind -> kind == TokenKind.RParenthesis))
      return new ListExpr(next().start(), Tag.Param, List.of());
    Expression ret = new ListExpr(start.start(), Tag.Param, expr2());
    next();
    return ret;
  }

  protected Tokens.Token next(){
    pre = token;
    if(!savedTokens.isEmpty())
      token = savedTokens.remove(0);
    else if(lexer.hasNext())
      token = lexer.next();
    else
      token = null;
    return pre;
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
    ensureLookahead(0);
    return hasTok() && tk.test(token().kind());
  }

  boolean peekTok(Predicate<Tokens.TokenKind> tk, Predicate<Tokens.TokenKind> tk1){
    ensureLookahead(2);
    return hasTok() && tk.test(token().kind()) && savedTokens.size() > 1 && tk1.test(token(1).kind());
  }

  /**
   * expr2 =  expr3 | expr2 "," expr3
   */
  private List<Expression> expr2(){
    List<Expression> ret = new LinkedList<>();
    ret.add(expr3());
    while(peekTok(tokenKind -> tokenKind == TokenKind.Comma)){
      next(); //
      ret.add(expr3());
    }
    return ret;
  }

  private BlockStatement compileBlock(TokenKind start, TokenKind end){
    if(peekTok(tk -> tk == start))
      next();
    else if(peekTok(tk -> tk == TokenKind.newline, tk -> tk == start)){
      next();
      next();
    }
    var ret = compileBlock(end);
    return ret;
  }

  /**
   * e_block = [ expression ]
   */
  private BlockStatement compileBlock(TokenKind end){
    final long pos = token().start();
    List<Tree> ret = new LinkedList<>();
    while(
      hasTok() &&
      !peekTok(tk -> tk == end) &&
      !peekTok(tk -> tk == TokenKind.newline, tk -> tk == end)
    ){
//      if(peekTok(tk -> tk != TokenKind.RParenthesis))
//        next();
      ret.add(expression());
      while (hasTok() && peekTok(tk -> tk == TokenKind.newline))
        next();
    }
    if(peekTok(tk -> tk == end))
      next();
    else if(peekTok(tk -> tk == TokenKind.newline, tk -> tk == end)){
      next();
      next();
    }
    return new BlockStatement(ret, pos);
  }

  /**
   * expr3 = identifier "=" expr3
   *       | add
   */
  private Expression expr3(){
    if(peekTok(tk -> tk == TokenKind.Id, tk -> tk == TokenKind.Assignment)) {
      Tokens.Token id = next();
      next(); // equal
      return new AssignmentExpression(new IdExpr(id.start(), Tag.Id, id.payload()), expr3(), id.start());
    }
    return add();
  }


  /**
   * expression = varDecl "=" expr4
   *            | expr3
   *            | e_fun
   */
  private Tree expression(){
    if(peekTok(tk -> tk == TokenKind.Fun)){
      System.out.println("parsing function");
      return compileFunc();
    }
    if(peekTok(tk -> tk == TokenKind.Let || tk == TokenKind.Val)){
      Tokens.Token decl = next();
      if(peekTok(tk -> tk != TokenKind.Id))
        error(token());
      Tokens.Token id = next();
      Modifiers modifiers = switch (decl.kind()){
        case Val -> new Modifiers.Builder()
            .Const()
            .build();
        case Let -> new Modifiers.Builder().build();
        default -> Modifiers.Empty;
      };
      if(peekTok(tk -> tk == TokenKind.Assignment)){
        System.out.println("create a assignment expression");
        var assign = next();
        return new AssignmentExpression(
            new VariableDecl(id.payload(), modifiers, id.start()),
            expr3(),
            assign.start()
        );
      }
    }
    return expr3();
  }

  void error(Tokens.Token tok){
    System.err.println("error in " + tok);
  }

  /**
   * one = identifier | literal | "(" expr3 ")" | call
   */
  Expression one(){
    Tokens.Token peek = token();
    return switch (peek.kind()){
      case IntLiteral,
          StringLiteral,
          DoubleLiteral,
          CharLiteral -> literal();

      case LBrace -> {
        System.out.println("doing >> ( ) << expression");
        next();
        Expression ret = expr3();
        // todo check
        next(); // ")"
        yield  ret;
      }
      case Id -> {
        if(peekTok(tk -> tk == TokenKind.Id, tk -> tk == TokenKind.LParenthesis))
          yield call();
        else yield new IdExpr(peek.start(), Tag.Id, next().payload());
      }
      default -> null; // error
    };
  }

  /**
   * unary = one | "+" unary | "-" unary
   */
  Expression unary(){
    if (peekTok( tk -> tk == TokenKind.Add || tk == TokenKind.Sub)){
      Tokens.Token pre = next();
      Expression unary = unary();
      return new Unary(kind2tag(pre.kind()), unary);
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
    Tokens.Token tok = next();
    System.out.println("parsing literal " + tok);
    return switch (tok.kind()) {
      case True -> LiteralExpression.ofStringify("true", LiteralExpression.Kind.Boolean);
      case False-> LiteralExpression.ofStringify("false", LiteralExpression.Kind.Boolean);
      case StringLiteral -> LiteralExpression.ofStringify(tok.payload(), LiteralExpression.Kind.String);
      case IntLiteral -> LiteralExpression.ofStringify(tok.payload(), LiteralExpression.Kind.Int);
      case DoubleLiteral -> LiteralExpression.ofStringify(tok.payload(), LiteralExpression.Kind.Double);
      default -> null;
    };
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
      left = (Expression) new BinaryOperateExpression(left, right, kind2tag(op.kind()))
          .setPos(op.start());
    }
    return left;
  }

  /**
   * multi = unary | multi "*" unary | multi "/" unary
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
      left = (Expression) new BinaryOperateExpression(left, right, kind2tag(op.kind()))
          .setPos(op.start());
    }
    return left;
  }

  Tag kind2tag(TokenKind kind){
    return switch (kind){
      case Add -> Tag.Add;
      case Multi -> Tag.Multi;
      case Sub -> Tag.Sub;
      case Div -> Tag.Div;
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

package com.silence.vmy.compiler;

import com.silence.vmy.compiler.Tokens.Token;
import com.silence.vmy.compiler.Tokens.TokenKind;
import com.silence.vmy.compiler.tree.*;
import com.silence.vmy.compiler.tree.Tree.Tag;
import com.silence.vmy.runtime.VmyRuntimeException;
import com.silence.vmy.tools.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public class GeneralParser implements Parser{
  private Lexer lexer;
  private Tokens.Token token;
  private Tokens.Token pre;
  private List<Tokens.Token> savedTokens = new LinkedList<>();

  GeneralParser(Lexer _lexer){
    this.lexer = _lexer;
    do_next(); // fill token
    ignoreAnnotation();
  }

  public static Parser create(Lexer lexer){ return new GeneralParser(lexer); }

  protected Tokens.Token next(){
    ignoreAnnotation();
    return do_next();
  }

  protected Tokens.Token do_next(){
    pre = token;
    if(!savedTokens.isEmpty()) // move cache to token
      token = savedTokens.remove(0);
    else if(lexer.hasNext()) // get token from lexer
      token = lexer.next();
    else // no token
      token = null;
    return pre;
  }

  protected Tokens.Token token(){ return token(0); }
  protected Tokens.Token token(int lookahead) {
    if(lookahead == 0) {
      if(token == null && hasTok()){
        token = do_next();
      }
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

  protected boolean hasTok(){ return Objects.nonNull(token) || !savedTokens.isEmpty() || lexer.hasNext(); }

  boolean peekTok(Predicate<Tokens.TokenKind> tk){
    ensureLookahead(0);
    return hasTok() && tk.test(token().kind());
  }

  boolean peekTok(Predicate<Tokens.TokenKind> tk, Predicate<Tokens.TokenKind> tk1){
    ensureLookahead(2);
    return hasTok() && tk.test(token().kind()) && savedTokens.size() > 1 && tk1.test(token(1).kind());
  }

  protected Tokens.Token next_must(TokenKind tk){
    if(hasTok() && peekTok(tokenKind -> tokenKind != tk))
      error("expect token kind : %s , current token is %s".formatted(tk, token.toString()));
    return do_next();
  }

  protected void ignore(TokenKind tokenKind){
    while(peekTok(tk -> tk == tokenKind)){
      next();
    }
  }

  private void ignoreEmptyLines(){
    ignoreAnnotation();
    while(peekTok(tk -> tk == TokenKind.newline)){
      ignore(TokenKind.newline);
      ignoreAnnotation();
    }
  }

  void error(String msg){
    Utils.error("error in " + msg);
    throw new LexicalException("<<error>>");
  }

  private Predicate<TokenKind> tokenkindIsEqual(TokenKind tk){
    return tokenkind -> tokenkind == tk;
  }

  @Override
  public Root parse() { return Trees.createCompileUnit(hasTok() ? compileBlock(TokenKind.EOF) : null); }

  // e_fun = fun identifier expr "{" e_block "}"
  private FunctionDecl compileFunc(){
    if(peekTok(tk -> tk != TokenKind.Fun))
      error(next().toString());
    Tokens.Token decl = next();
    String name = null;
    if(peekTok(tk -> tk == TokenKind.Id))
      name = next().payload();
    return new FunctionDecl(
        name,
        parameters().body(),
        peekTok(tk -> tk == TokenKind.Colon) ? parsingType() : null,
        compileBlock(TokenKind.LBrace, TokenKind.RBrace),
        decl.start()
    );
  }

  // expr = "(" ")" | "(" expr2 ")"
  private ListExpr<? extends Expression> expr(){
    if(token().kind() != TokenKind.LParenthesis){
      throw new LexicalException("expression error position %d" + token());
    }

    Tokens.Token start = next();
    if(peekTok(kind -> kind == TokenKind.RParenthesis))
      return new ListExpr<>(next().start(), Tag.Param, List.of());
    ListExpr<?> ret = new ListExpr<>(start.start(), Tag.Param, expr2());
    next();
    return ret;
  }

  // parameters_expr = "(" ")" | "(" parameter_expr ["," parameter_expr] ")"
  // parameter_expr = identifier ":" typeDecl
  private ListExpr<VariableDecl> parameters(){
    if(token().kind() != TokenKind.LParenthesis){
      throw new LexicalException("expression error position %d" + token());
    }

    Tokens.Token start = next();
    if(peekTok(kind -> kind == TokenKind.RParenthesis))
      return new ListExpr<>(next().start(), Tag.Param, List.of());
    List<VariableDecl> ls = new LinkedList<>();
    ls.add(parameter());
    while(peekTok(tk -> tk == TokenKind.Comma)){
      next_must(TokenKind.Comma);
      ls.add(parameter());
    }
    next_must(TokenKind.RParenthesis);
    return new ListExpr<>(start.start(), Tag.Param, ls);
  }

  private VariableDecl parameter(){
    Tokens.Token id = next_must(TokenKind.Id);
    return new VariableDecl(id.payload(), Modifiers.Const, parsingType(), id.start());
  }

  // expr2 =  expr3
  //        | expr2 "," expr3
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
    ignore(TokenKind.newline);
    next_must(start);
    return compileBlock(end);
  }

  // e_block = [ expression ]
  private BlockStatement compileBlock(TokenKind end){
    final long pos = token().start();
    List<Tree> ret = new LinkedList<>();
    ignoreEmptyLines();
    ignoreAnnotation();
    while(
      hasTok() &&
      !peekTok(tk -> tk == end) &&
      !peekTok(tk -> tk == TokenKind.newline, tk -> tk == end)
    ){
      ret.add(expression());
      ignoreEmptyLines();
    }
    ignoreEmptyLines();
    next_must(end);
    return new BlockStatement(ret, pos);
  }

   // expr3 = identifier "=" expr3
   //       | identifier "+=" expr3
   //       | identifier "-=" expr3
   //       | identifier "*=" expr3
   //       | identifier "/=" expr3
   //       | expression4
  private Expression expr3(){
    if(peekTok(tk -> tk == TokenKind.Id, tk -> tk == TokenKind.Assignment)) {
      Tokens.Token id = next();
      next_must(TokenKind.Assignment);
      return new AssignmentExpression(new IdExpr(id.start(), Tag.Id, id.payload()), expr3(), id.start());
    }
    Function<TokenKind,Boolean> is_assign_op = tk -> switch (tk){
      case SubEqual, DivEqual, MultiEqual, AddEqual -> true;
      default -> false;
    };
    if(peekTok(tk -> tk == TokenKind.Id, is_assign_op::apply)){
      Tokens.Token id = next();
      Tokens.Token op = next();
      return new BinaryOperateExpression(
          new IdExpr(id.start(), Tag.Id, id.payload()),
          expr3(),
          kind2tag(op.kind())
      );
    }
    return expression4();
  }

  // comparing = add
  //           | add "++" concat
  //           | add comparingOp add
  // comparingOp = "=="
  //             | "!=" 
  //             | "<" 
  //             | "<=" 
  //             | ">" 
  //             | ">=" 
  private Expression expression4(){
    Expression addExpression = add();
    if( /* add "++" concat */
      peekTok(tokenkindIsEqual(TokenKind.Concat)))
    {
      next(); // drop "++"
      return new BinaryOperateExpression(
        addExpression, 
        concat(), 
        Tag.Concat);
    }

    if(/* add comparingOp add */
      peekTok(this::isComparingOp))
    {
      Token comparingOp = next();
      return new BinaryOperateExpression(
        addExpression, 
        add(), 
        kind2tag(comparingOp.kind()));
    }
    // add
    return addExpression;
  }

  private boolean isComparingOp(TokenKind tk){
    return switch (tk) {
      case Equal, NotEqual, Less, Greater, Le, Ge -> true;
      default -> false;
    };
  }

  // concat = add
  //         | add "++" concat
  private Expression concat(){
    Expression firstExpr = add();
    if(hasTok() && peekTok(tk -> tk == TokenKind.Concat)){
      next_must(TokenKind.Concat);
      Expression right = concat();
      // concat expression
      return new BinaryOperateExpression(firstExpr, right, Tag.Concat);
    }
    // add expression
    return firstExpr;
  }

  // type_decl = ":" identifier
  private TypeExpr parsingType(){
    var start = next_must(TokenKind.Colon);
    Tokens.Token id = next_must(TokenKind.Id);
    return new TypeExpr(start.start(), Tag.TypeDecl, id.payload());
  }

  private void ignoreAnnotation() {
    while (peekTok(tk -> tk == TokenKind.Annotation)){
      next_must(TokenKind.Annotation);
      while (peekTok(tk -> tk != TokenKind.newline && tk != TokenKind.EOF))
        do_next(); // remove token of this line
      do_next(); // newline & end of file
    }
  }

   //expression = varDecl "=" expr4
   //            | expr3
   //            | e_fun
   //            | "return" expr3
  //             | ifStatement
  private Expression expression(){
    if(peekTok(tk -> tk == TokenKind.Fun)){/* function */
      return compileFunc();
    }
    if( /* variable declaration */
        peekTok(tk -> tk == TokenKind.Let || tk == TokenKind.Val)){

      Tokens.Token decl = next();
      if(peekTok(tk -> tk != TokenKind.Id))
        error(token().toString());
      Tokens.Token id = next();
      Modifiers modifiers = switch (decl.kind()){
        case Val -> new Modifiers.Builder()
            .Const()
            .build();
        case Let -> new Modifiers.Builder().build();
        default -> Modifiers.Empty;
      };
      TypeExpr type = peekTok(tk -> tk == TokenKind.Colon) ? parsingType() : null;
      if(peekTok(tk -> tk == TokenKind.Assignment)){
        var assign = next();
        return new AssignmentExpression(
            new VariableDecl(id.payload(), modifiers, type,id.start()),
            expr3(),
            assign.start()
        );
      }
    }

    // return expression
    if(peekTok(tk -> tk == TokenKind.Return)){
      Tokens.Token ret = next();
      return new ReturnExpr(ret.start(), null, expr3());
    }

    if(peekTok(tk -> tk == TokenKind.If))
      return if_statement();
    //expr3
    return expr3();
  }

  // one = identifier
  //     | literal
  //     | "(" expr3 ")"
  //     | call
  //     | arr
  Expression one(){
    Tokens.Token peek = token();
    return switch (peek.kind()){
      case IntLiteral,
          StringLiteral,
          DoubleLiteral,
          True,
          False,
          CharLiteral -> literal();

      case LParenthesis -> {
        next_must(TokenKind.LParenthesis);
        Expression ret = expr3();
        next_must(TokenKind.RParenthesis);
        yield  ret;
      }

      case Id -> {
        if(peekTok(tk -> tk == TokenKind.Id, tk -> tk == TokenKind.LParenthesis))
          yield call();
        else yield new IdExpr(peek.start(), Tag.Id, next().payload());
      }

      case ArrOpen -> arrExpression();

      default -> null; // error
    };
  }

  // arr = "[" one oneRest "]"
  // oneRest = { "," one }
  private Expression arrExpression(){
    Token arrayToken = next_must(TokenKind.ArrOpen);
    List<Expression> elements = new ArrayList<>();
    elements.add(one());
    while(peekTok(tokenkindIsEqual(TokenKind.ArrClose).negate() /* not equal */)){
      next_must(TokenKind.Comma);
      elements.add(one());
    }
    next_must(TokenKind.ArrClose);
    return new ArrExpression(elements, Tag.Arr, arrayToken.start());
  }

   // unary = one
   //       | "+" unary
   //       | "-" unary
  Expression unary(){
    if (peekTok( tk -> tk == TokenKind.Add || tk == TokenKind.Sub)){
      Tokens.Token pre = next();
      Expression unary = unary();
      return new Unary(kind2tag(pre.kind()), unary);
    }
    return one();
  }

   // literal = "true" | "false"
   //         | numberLiteral
   //         | stringLiteral
   //         | charLiteral
   //         | functionLiteral
  Expression literal(){
    Tokens.Token tok = next();
    return switch (tok.kind()) {
      case True -> LiteralExpression.ofStringify("true", LiteralExpression.Kind.Boolean);
      case False-> LiteralExpression.ofStringify("false", LiteralExpression.Kind.Boolean);
      case StringLiteral -> LiteralExpression.ofStringify(tok.payload(), LiteralExpression.Kind.String);
      case IntLiteral -> LiteralExpression.ofStringify(tok.payload(), LiteralExpression.Kind.Int);
      case DoubleLiteral -> LiteralExpression.ofStringify(tok.payload(), LiteralExpression.Kind.Double);
      default -> throw new VmyRuntimeException("literal expression not support start token : " + tok);
    };
  }

  // private void ifThenIgnore(TokenKind tk){
  //   if(peekTok(tokenKind -> tk == tokenKind)){
  //     ignore(tk);
  //   }
  // }

  // if_statement = "if" "(" expression ")"
  private Statement if_statement(){
    ConditionStatement ifHead = statementTemplate(TokenKind.If);
    // parsing elif
    List<ConditionStatement> elifs = new ArrayList<>();
    ignoreEmptyLines();
    while (peekTok(tk -> TokenKind.Elif == tk)){
      elifs.add(statementTemplate(TokenKind.Elif));
      ignoreEmptyLines();
    }

    // parsing else
    BlockStatement elseStatement = null;
    if(peekTok(tk -> tk == TokenKind.Else)){
      next(); // else
      /*
      * two case:
      * 1.
      *  else {
      * 2.
      * else
      * newline
      * ...
      * newline
      * {
      **/
      ignoreEmptyLines();
      elseStatement = compileBlock(TokenKind.LBrace, TokenKind.RBrace);
    }
    return new IfStatement(ifHead, elifs, elseStatement);
  }

  private Tag tempateStartkind2Tag(TokenKind tokenKind){
    return switch (tokenKind){
      case If -> Tag.If;
      case Elif -> Tag.Elif;
      default -> null;
    };
  }

  // statementTemplate = "startToken" "(" expression ")"
  private ConditionStatement statementTemplate(TokenKind startTokenkind){
    Tokens.Token startToken = next_must(startTokenkind);
    next_must(TokenKind.LParenthesis);
    Tree expression = expression();
    next_must(TokenKind.RParenthesis);
    return new ConditionStatement(
        expression,
        compileBlock(TokenKind.LBrace, TokenKind.RBrace),
        tempateStartkind2Tag(startTokenkind),
        startToken.start());
  }


   // add = multi
   //    | multi "+" multi
   //    | multi "-" multi
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

  // multi = unary
  //       | multi "*" unary
  //       | multi "/" unary
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
      case DivEqual -> Tag.DivEqual;
      case SubEqual -> Tag.SubEqual;
      case MultiEqual -> Tag.MultiEqual;
      case AddEqual -> Tag.AddEqual;
      case Equal -> Tag.Equal;
      case NotEqual -> Tag.NotEqual;
      case Less -> Tag.Less;
      case Greater -> Tag.Greater;
      case Le -> Tag.Le;
      case Ge -> Tag.Ge;
      default -> null;
    };
  }

  /**
   * call = identifier expr
   */
  private Expression call(){
    Tokens.Token id = next_must(TokenKind.Id);
    ListExpr<? extends Expression> params = expr();
    return CallExpr.create(id.start(), id.payload(), params);
  }

}

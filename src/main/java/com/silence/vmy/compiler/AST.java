package com.silence.vmy.compiler;

import com.silence.vmy.compiler.visitor.ASTProcessingException;
import com.silence.vmy.compiler.tree.*;
import com.silence.vmy.tools.Utils;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AST {
  private AST(){}

  public enum LiteralKind{
    Int,
    Double,
    Char,
    Bool,
    String;
  }

  public static class VmyAST implements Root {
    public Tree root;
  }

  // main for support old version test
  public static VmyAST build(List<Token> tokens){
    Stack<String> operatorStack = new Stack<>();
    Stack<Tree> nodesStack = new Stack<>();
    Iterator<Token> tokenIterator = tokens.iterator();
    com.silence.vmy.compiler.Scanner scanner = new Scanners.VmyScanner(tokens);
    TokenHandler handler = getTokenHandler();
    while(tokenIterator.hasNext()){
      handler.handle(scanner.next(), scanner, operatorStack, nodesStack);
    }
    VmyAST ast = new VmyAST();

    if(!nodesStack.isEmpty()){
      Tree merge = nodesStack.pop();
      while(
        !operatorStack.isEmpty() &&
        !nodesStack.isEmpty()
      ){
          final String operator = operatorStack.pop();
          final Tree asLeft = nodesStack.pop();
          merge = new BinaryOperatorNode(operator, asLeft, merge);
      }
      if(!nodesStack.isEmpty() || !operatorStack.isEmpty())
        throw new ASTProcessingException("expression wrong");
      ast.root = merge;
    }
    return ast;
  }

  // new version
  public static VmyAST build(com.silence.vmy.compiler.Scanner scanner){
    TokenHistoryRecorder recorder = new FixedSizeCapabilityTokenRecorder(3);
    scanner.register(recorder, false);
    Stack<String> operatorStack = new Stack<>();
    Stack<Tree> nodesStack = new Stack<>();
    TokenHandler handler = getTokenHandler(recorder);
    while(scanner.hasNext()){
      handler.handle(scanner.next(), scanner, operatorStack, nodesStack);
    }
    VmyAST ast = new VmyAST();
    ast.root = merge_linear_nodes(nodesStack);
    return ast;
  }

  private static Tree merge_linear_nodes(List<Tree> nodes){
//    return new Process
    return new BlockNode(nodes);
  }

  public interface TokenHandler{
    void handle(Token token, com.silence.vmy.compiler.Scanner remains, Stack<String> operatorStack, Stack<Tree> nodesStack);
  }


  private static 
  abstract class BaseHandler
    implements TokenHandler, 
    Utils.Recursive,
          TokenHistoryRecorderGetter,
      Utils.PeekTokenAbility
  {
    private BaseHandler next;

    private BaseHandler head;

    private TokenHistoryRecorder tokenHistoryRecorder;

    private com.silence.vmy.compiler.Scanner scanner = null;

    public void setNext(final BaseHandler _next){
      next = _next;
    }

    public void setHead(BaseHandler _head){
      head = _head;
    }

    final protected void recall(
      Token token, 
      com.silence.vmy.compiler.Scanner remains,
      Stack<String> operatorStack, 
      Stack<Tree> nodesStack
    ){
      if(Objects.isNull(head))
        throw new Utils.RecursiveException("can't do recall head is not exists!");
      head.handle(token, remains, operatorStack, nodesStack);
    }

    public void setTokenRecorder(TokenHistoryRecorder recorder){
      tokenHistoryRecorder = recorder;
    }

    @Override
    public TokenHistoryRecorder getTokenRecorder(){
      return tokenHistoryRecorder;
    }

    @Override
    final public void handle(
      Token token, 
      com.silence.vmy.compiler.Scanner remains,
      Stack<String> operatorStack, 
      Stack<Tree> nodesStack
    ){
      try {
        if(Objects.isNull(scanner))
          scanner = remains;
        if(canHandle(token, operatorStack, nodesStack))
          doHandle(token, remains, operatorStack, nodesStack);
        else if(Objects.nonNull(next))
          next.handle(token, remains, operatorStack, nodesStack);
      }catch (Exception e){
        e.printStackTrace();
        throw new ASTProcessingException(String.format("%s : at token %s", e.getMessage(), token));
      }
    }

    public abstract boolean canHandle(
      Token token, 
      Stack<String> operatorStack, 
      Stack<Tree> nodesStack
    );

    public abstract void doHandle(
      Token token, 
      com.silence.vmy.compiler.Scanner remains,
      Stack<String> operatorStack, 
      Stack<Tree> nodesStack
    );

    @Override
    public Token peek() {
      return scanner.peek();
    }
  }

  private static class NumberHandler extends BaseHandler{

    @Override
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<Tree> nodesStack) {
      return token.tag == Token.INT_V || token.tag == Token.DOUBLE_V;
    }

    @Override
    public void doHandle(
      Token token, 
      com.silence.vmy.compiler.Scanner remains,
      Stack<String> operatorStack, 
      Stack<Tree> nodesStack
    ) {

      nodesStack.add(
        token.tag == Token.DOUBLE_V ? 
        new ValNode( Double.parseDouble(token.value) ) :
        new ValNode( Integer.parseInt(token.value) )
      );

    }

  }

  private static class CallHandler extends Tool {
    @Override
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<Tree> nodesStack) {
      return (token.tag == Token.BuiltinCall || token.tag == Token.Identifier) && Utils.equal(peek().value, Identifiers.OpenParenthesis);
    }

    @Override
    public void doHandle(Token token, com.silence.vmy.compiler.Scanner remains, Stack<String> operatorStack, Stack<Tree> nodesStack) {

        Token should_be_open_parenthesis;
        if(!remains.hasNext() || !operatorEquals(Identifiers.OpenParenthesis, (should_be_open_parenthesis = remains.next())))
          throw new ASTProcessingException("builtin call " + token.value + " should be followed with open parenthesis '('");
        if(operatorEquals(Identifiers.ClosingParenthesis, remains.peek())){
          // no content, empty call like : print()
          remains.next();
          nodesStack.add(new CallNode(token.value, new ListExpression(List.of())));
          return;
        }

        Token start_token = should_be_open_parenthesis;
        while(
            remains.hasNext() &&
            (operatorStack.isEmpty() || !Utils.equal(start_token.value, Identifiers.ClosingParenthesis))
        ){
          travel_back_build(
              start_token,
              remains,
              operatorStack,
              nodesStack,
              Set.of(Identifiers.Comma, Identifiers.ClosingParenthesis),
              Set.of(Identifiers.Comma, Identifiers.OpenParenthesis)
          );
          start_token = new Token(-1, operatorStack.pop());
        }

        // last operators must be like this : ( , , )
        if(!Utils.equal(start_token.value, Identifiers.ClosingParenthesis))
          throw new ASTProcessingException("there is no closing parenthesis when handle builtin call " + token.value);
        LinkedList<Tree> params = new LinkedList<>();
        while(
            !operatorStack.isEmpty() &&
            !nodesStack.isEmpty() &&
            !Utils.equal( operatorStack.peek(), Identifiers.OpenParenthesis)
        ){
          if(
              !Utils.equal(operatorStack.pop(), Identifiers.Comma)
          ) throw new ASTProcessingException("error when merge builtin call " + token.value);
          // do merge
          params.addFirst(nodesStack.pop());
        }
        operatorStack.pop(); // pop the "("
        params.addFirst(nodesStack.pop());
        nodesStack.add(new CallNode(token.value, new ListExpression(params)));

    }
  }

  private static class OperatorHandler extends Tool{

    @Override
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<Tree> nodesStack) {
      return token.tag == Token.BuiltinCall ||
          (
            token.tag == Token.Identifier &&
            ( Identifiers.commonIdentifiers.contains(token.value.charAt(0)) || is_operator(token) )
          );
    }

    private boolean is_operator(Token token){
      return Identifiers.operatorCharacters.contains(token.value.charAt(0)) || Utils.equal(token.value, "=");
    }

    @Override
    public void doHandle(Token token, com.silence.vmy.compiler.Scanner remains, Stack<String> operatorStack, Stack<Tree> nodesStack) {

      if(
          /* + */
          operatorEquals(Identifiers.ADD, token)||
          /* - */
          operatorEquals(Identifiers.SUB, token)||
          /* ++ */
          operatorEquals(Identifiers.Concat, token)
      ) {

        switch(token.value){
          case Identifiers.SUB:
            TokenHistoryRecorder recorder = getTokenRecorder();
            if(
              Objects.nonNull(recorder) && 
              recorder.has_history() && 
              is_operator( recorder.get(1) )
            ){
              // todo
              final Token should_be_number = remains.next();
              final int flag = is_digit(should_be_number.value);
              if(flag == 0)
                throw new ASTProcessingException("went error when process negative number");
              String negative_value = token.value + should_be_number.value;
              nodesStack.add(
                flag == 1 ? 
                new NumberLiteral(Integer.parseInt(negative_value) ) : 
                new NumberLiteral( Double.parseDouble(negative_value) )
              );
              break;
            }
          case Identifiers.Concat:
//            nodesStack.add(new BinaryOperatorNode(token.value, nodesStack.pop(), remains.next()));
            Tree left = nodesStack.pop();
            recall(remains.next(), remains, operatorStack, nodesStack);
            nodesStack.add(new BinaryOperatorNode(token.value, left, nodesStack.pop()));
            break;
          default:
            operatorStack.add(token.value);
            break;
        }

      } else if(/* >, <, <= , >=, == */ Identifiers.BoolOperators.contains(token.value)){

        /**
         * <p>
         *   BoolOperators must be like this:
         * left-expression operator right-expression
         * </p>
         * <p>example : a + c < b + a </p>
         */
        // merge the right side of operator
        travel_back_build(
            token,
            remains,
            operatorStack,
            nodesStack,
            /* end with ), newline, } */Set.of(Identifiers.ClosingParenthesis, "\n", "\r\n"),
            /* start with  */Identifiers.BoolOperators,
            false
        );
        operatorStack.pop();

        Tree right = nodesStack.pop();
        nodesStack.add(
            new BinaryOperatorNode(
                operatorStack.pop(), // operator
                merge_back( /* merge the left side of operator */
                    operatorStack,
                    nodesStack,
                    (nodes, op_stack) -> Utils.equal(op_stack.peek(), Identifiers.OpenParenthesis)
                ),
                right
            )
        );

      }
//      else if(/* a call like : print(1) */
//          token.tag == Token.BuiltinCall ||
//          (remains.hasNext() && Utils.equal(remains.peek().value, Identifiers.OpenParenthesis))
//      ){
//
//        Token should_be_open_parenthesis;
//        if(!remains.hasNext() || !operatorEquals(Identifiers.OpenParenthesis, (should_be_open_parenthesis = remains.next())))
//          throw new ASTProcessingException("builtin call " + token.value + " should be followed with open parenthesis '('");
//        if(operatorEquals(Identifiers.ClosingParenthesis, remains.peek())){
//          // no content, empty call like : print()
//          remains.next();
//          nodesStack.add(new CallNode(token.value, new ListExpression(List.of())));
//          return;
//        }
//
//        Token start_token = should_be_open_parenthesis;
//        while(
//            remains.hasNext() &&
//            (operatorStack.isEmpty() || !Utils.equal(start_token.value, Identifiers.ClosingParenthesis))
//        ){
//          travel_back_build(
//              start_token,
//              remains,
//              operatorStack,
//              nodesStack,
//              Set.of(Identifiers.Comma, Identifiers.ClosingParenthesis),
//              Set.of(Identifiers.Comma, Identifiers.OpenParenthesis)
//          );
//          start_token = new Token(-1, operatorStack.pop());
//        }
//
//        // last operators must be like this : ( , , )
//        if(!Utils.equal(start_token.value, Identifiers.ClosingParenthesis))
//          throw new ASTProcessingException("there is no closing parenthesis when handle builtin call " + token.value);
//        LinkedList<ASTNode> params = new LinkedList<>();
//        while(
//            !operatorStack.isEmpty() &&
//            !nodesStack.isEmpty() &&
//            !Utils.equal( operatorStack.peek(), Identifiers.OpenParenthesis)
//        ){
//          if(
//              !Utils.equal(operatorStack.pop(), Identifiers.Comma)
//          ) throw new ASTProcessingException("error when merge builtin call " + token.value);
//          // do merge
//          params.addFirst(nodesStack.pop());
//        }
//        operatorStack.pop(); // pop the "("
//        params.addFirst(nodesStack.pop());
//        nodesStack.add(new CallNode(token.value, new ListExpression(params)));
//      }
      else if(operatorEquals(Identifiers.MULTI, token) || operatorEquals(Identifiers.DIVIDE, token) ){

        if(!remains.hasNext())
          throw new ASTProcessingException("*(multiply) doesn't have right side");
        if(nodesStack.isEmpty())
          throw new ASTProcessingException("*(multiply) left side not exists");
        Tree left = nodesStack.pop();
        recall(remains.next(), remains, operatorStack, nodesStack);
        nodesStack.add(new BinaryOperatorNode(token.value, left, nodesStack.pop()));

      }else if(operatorEquals(Identifiers.OpenParenthesis, token)){

        travel_back_build(
            token,
            remains,
            operatorStack,
            nodesStack,
            Set.of(Identifiers.ClosingParenthesis),
            Set.of(Identifiers.OpenParenthesis)
        );
        // remove "(" and ")"
        if(
          !Utils.equal(operatorStack.pop(), Identifiers.ClosingParenthesis) ||
          !Utils.equal(operatorStack.pop(), Identifiers.OpenParenthesis)
        ) throw new ASTProcessingException("Parenthesis process error");

      }else
        throw new ASTProcessingException("not support operator " + token.value);
    }

  }

  private static abstract class Tool extends BaseHandler {

    // a default option for remove end-op
    protected void travel_back_build(
        Token token,
        com.silence.vmy.compiler.Scanner remains,
        Stack<String> operation_stack,
        Stack<Tree> nodes_stack,
        Set<String> end_op,
        Set<String> start_op
    ){
      travel_back_build(
          token,
          remains,
          operation_stack,
          nodes_stack,
          end_op,
          start_op,
          true
      );
    }

    // travel build like : binary operation
    // when in front specific node , merge all this traveled node
    // just like : combine the calculating the formula : 1 + 2 * () + 3
    // add option to determined if it should remove the end_op from token
    protected void travel_back_build(
        Token token,
        com.silence.vmy.compiler.Scanner remains,
        Stack<String> operation_stack,
        Stack<Tree> nodes_stack,
        Set<String> end_op,
        Set<String> start_op,
        boolean remove_end
    ) {
      // logic
      // 1. add the start operator
      // 2. if next is end_op , just add empty node
      // 3. handle thing between start_op and end_op
      // 4. merge all node between start_op and end_op
      // 5. add end_op to operation stack and add merged node to nodes stack
      operation_stack.add(token.value);
      if(/* not content */end_op.contains(remains.peek().value)){
        // add an empty node
        operation_stack.add(remains.next().value);
        nodes_stack.add(new EmptyNode());
        return;
      }

      while(
          remains.hasNext() &&
          !end_op.contains(remains.peek().value)
      ) recall(remains.next(), remains, operation_stack, nodes_stack);

      if(!remains.hasNext() || !end_op.contains(remains.peek().value))
        throw new ASTProcessingException("there is no end_op for " + Utils.collection_to_string(end_op) );
      Token next_token = remove_end ? next_token = remains.next() : remains.peek();
      // merge node and add to node stack
      nodes_stack.add(
          merge_back(
            operation_stack,
            nodes_stack,
            (nodes, op_stack) -> Utils.equal( token.value, op_stack.peek())
          )
      );
      operation_stack.add(next_token.value);
    }

    /**
     * merge node till satisfied end_condition
     * @param operation_stack
     * @param nodes_stack
     * @param end_condition
     * @return
     */
    protected Tree merge_back(
        Stack<String> operation_stack,
        Stack<Tree> nodes_stack,
        BiPredicate<Stack<Tree>, Stack<String>> end_condition
    ){
      if(nodes_stack.isEmpty())
        throw new ASTProcessingException("merge node error , because node_stack isEmpty");

      Tree merge_node = nodes_stack.pop();

      while(
          !operation_stack.isEmpty() &&
          !nodes_stack.isEmpty() &&
          !end_condition.test(nodes_stack, operation_stack)
      ){
        final String operator = operation_stack.pop();
        final Tree asLeft = nodes_stack.pop();
        merge_node = mergeTwoNodes(asLeft, merge_node, operator);
      }
      if(operation_stack.isEmpty() || !end_condition.test(nodes_stack, operation_stack))
        throw new ASTProcessingException("error at processing merge back , not satisfied end_condition");
      return merge_node;
    }

    // double : 2
    // int : 1
    // not digit: 0
    protected int is_digit(String _value){
      if(_value.length() == 0) return 0;
      int walk = 0;
      while(walk < _value.length() && !Utils.equal(_value.charAt(walk), '.'))
        if(!Character.isDigit(_value.charAt(walk++))) return 0;
      if(walk < _value.length() && Utils.equal(_value.charAt(walk), '.')){
        walk++;
        while(walk < _value.length())
          if(!Character.isDigit(_value.charAt(walk++))) return 0;
        return 2;
      }
      return 1; // 1
    }

    // handle code like this :
    // name (params) {
    // .....
    // }
    protected void handle_name_params_and_block(
        Token name,
        com.silence.vmy.compiler.Scanner remains,
        Stack<String> operator_stack,
        Stack<Tree> nodes_stack,
        BiPredicate<Token,Token> pre_process_params_check
    ){

      Token should_be_open_parenthesis = remains.next();
      if(
        Objects.nonNull(pre_process_params_check) &&
        !pre_process_params_check.test(should_be_open_parenthesis, remains.peek())
      ) return;

      // it should handle "(...)"
      // @see OperatorHandler , condition is OpenParenthesis
      recall(should_be_open_parenthesis, remains, operator_stack, nodes_stack);
      if(!remains.hasNext())
        throw new ASTProcessingException(name.value + " should has following code ");

      // it should handle "{....}"
      // @see BlockHandler
      recall(remains.next(), remains, operator_stack, nodes_stack);

    }

    /**
     * remove EOL till next token is not.
     * @param remains
     */
    protected void remove_end_of_line(com.silence.vmy.compiler.Scanner remains){
      while(remains.hasNext() && Utils.isEOL(remains.peek()))
        remains.next();
    }
  }


  // handle name like variable name
  private static class VariableNameHandler extends Tool{
    @Override
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<Tree> nodesStack) {
      return token.tag == Token.Identifier &&
          /* if any char not in Identifiers.identifiers, result will be less than 0, if this is necessary */
          token.value.chars()
              .reduce(
                  0,
                  (old, el) -> (Identifiers.identifiers.contains((char)el) ? 0 : -1) + old
              ) == 0 ;
    }

    @Override
    public void doHandle(
      Token token, 
      com.silence.vmy.compiler.Scanner remains,
      Stack<String> operatorStack, 
      Stack<Tree> nodesStack
    ) {

      switch (token.value){
        case Identifiers.Return -> {
//          travel_back_build();
          while(
              remains.hasNext() &&
              remains.peek().tag != Token.NewLine &&
              !Utils.equal(remains.peek().value, Identifiers.ClosingBrace)
          ) recall(remains.next(), remains, operatorStack, nodesStack);

          nodesStack.add(new Return(
            merge_back(
                operatorStack,
                nodesStack,
                (nodes, op_stack) -> {
                  String op = op_stack.peek();
                  return Utils.equal(op, Identifiers.NewLine) || Utils.equal(op, Identifiers.ClosingParenthesis);
                })
          ));
        }
        default -> nodesStack.add(new IdentifierNode(token.value));
      }

    }
  }

  private static class LiteralHandler extends Tool{
    @Override
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<Tree> nodesStack) {
      return token.tag == Token.Literal;
    }

    @Override
    public void doHandle(Token token, com.silence.vmy.compiler.Scanner remains, Stack<String> operatorStack, Stack<Tree> nodesStack) {

      // currently, it only needs to handle the string literal and bool literal
      // the Int and Double literal it handled by ValHandler
      int digit_flag;
      if(/* bool literal */
          Utils.equal(token.value, Identifiers.True) ||
          Utils.equal(token.value, Identifiers.False)
      ) nodesStack.add(new BoolLiteral(Utils.equal( token.value, Identifiers.True)));
      else if(/* number literal : int or double */
          (digit_flag = is_digit(token.value)) != 0
      ){
        nodesStack.add( digit_flag == 1 ?
            new NumberLiteral(Integer.parseInt(token.value)) :
            new NumberLiteral(Double.parseDouble(token.value))
        );
      } else /* string literal : "..." */
        nodesStack.add(new StringLiteral(token.value.substring(1, token.value.length() - 1)));

    }

  }

  // handle the expression like
  //    let a : Int = 2 or
  //    b = 1
  private static class AssignmentHandler extends BaseHandler {
    @Override
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<Tree> nodesStack) {
      return token.tag == Token.Assignment;
    }

    @Override
    public void doHandle(Token token, com.silence.vmy.compiler.Scanner remains, Stack<String> operatorStack, Stack<Tree> nodesStack) {

      // 1 get the variable name or a declaration
      Tree variable;
      if(
        nodesStack.isEmpty() ||
        (
          !((variable = nodesStack.pop()) instanceof DeclareNode) &&
          !(variable instanceof IdentifierNode)
        )
      ) throw new ASTProcessingException("assignment has no variable or declare expression");

      operatorStack.add(token.value);
      while(
        remains.hasNext() &&
        ( remains.peek().tag != Token.NewLine && !Utils.equal(remains.peek().value, Identifiers.ClosingBrace))
      ) recall(remains.next(), remains, operatorStack, nodesStack);

      // build it
      if(nodesStack.isEmpty())
        throw  new ASTProcessingException("assignment has no value expression");
      Tree the_value = nodesStack.pop();
      while(!operatorStack.isEmpty() && !Objects.equals( operatorStack.peek(), Identifiers.Assignment)){
        the_value = mergeTwoNodes(nodesStack.pop(), the_value, operatorStack.pop());
      }
      nodesStack.add(new AssignNode(variable, the_value));
      operatorStack.pop();

    }
  }

  // handle declaration expression, like
  //      let a , or
  //      let a : Int , or
  //      val a , or
  //      val a : Int
  private static class DeclarationHandler extends BaseHandler {
    @Override
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<Tree> nodesStack) {
      return token.tag == Token.Declaration;
    }

    @Override
    public void doHandle(Token token, com.silence.vmy.compiler.Scanner remains, Stack<String> operatorStack, Stack<Tree> nodesStack) {

      Token identifier;
      if((identifier = remains.next()).tag != Token.Identifier)
        throw new ASTProcessingException("declaration has no right identifier " + identifier.value);
      if(remains.hasNext() && Objects.equals( remains.peek().value, Identifiers.Colon)){
        remains.next();
        if(remains.hasNext() && remains.peek().tag != Token.Identifier)
          throw new ASTProcessingException(remains.peek().value + " is not a valid type");
        Token type = remains.next();
        nodesStack.add(new DeclareNode(token.value, new IdentifierNode(identifier.value) , type.value));
      }else
        nodesStack.add(new DeclareNode(token.value, new IdentifierNode(identifier.value)));

    }
  }

  /**
   * handle code block start at "{" and closed at "}"
   */
  private static class BlockHandler extends Tool{
    @Override
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<Tree> nodesStack) {
      return Utils.equal(token.value, Identifiers.OpenBrace);
    }

    @Override
    public void doHandle(Token token, com.silence.vmy.compiler.Scanner remains, Stack<String> operatorStack, Stack<Tree> nodesStack) {

      // no content
      if(operatorEquals(Identifiers.ClosingBrace, remains.peek())){
        remains.next();
        nodesStack.add(new BlockNode(List.of()));
        return;
      }

      Token start_token = token;
      Set<String> end_ops = Set.of(Identifiers.ClosingBrace, "\n", "\r\n");
      Set<String> start_ops = Set.of(Identifiers.OpenBrace, "\n", "\r\n");

      // handle each line, group each line as one node
      while (
          remains.hasNext() &&
          ( operatorStack.isEmpty() || !Utils.equal(start_token.value, Identifiers.ClosingBrace) )
      ){
        travel_back_build(
            start_token,
            remains,
            operatorStack,
            nodesStack,
            end_ops,
            start_ops
        );
        start_token = new Token(-1, operatorStack.pop());
      }

      // last operators must be like this : { ... new-line .... new-line ... }
      if(!Utils.equal(start_token.value, Identifiers.ClosingBrace))
        throw new ASTProcessingException("there is no closing parenthesis when handle builtin call " + token.value);

      LinkedList<Tree> params = new LinkedList<>();
      // add all line node to list
      Tree temp_node;
      while(
          !operatorStack.isEmpty() &&
          !nodesStack.isEmpty() &&
          !Utils.equal( operatorStack.peek(), Identifiers.OpenBrace)
      ){
        if(!Set.of("\n","\r\n").contains(operatorStack.pop()))
          throw new ASTProcessingException("error when merge builtin call " + token.value);
        // do merge
        if(!((temp_node = nodesStack.pop()) instanceof EmptyNode))
          params.addFirst(temp_node);
      }
      operatorStack.pop(); // pop the "{"
      if(!((temp_node = nodesStack.pop()) instanceof EmptyNode))
        params.addFirst(temp_node);
      nodesStack.add(new BlockNode(params));
    }
  }

  /**
   * while loop
   */
  private static class WhileHandler extends Tool {

    @Override
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<Tree> nodesStack) {
      return Utils.equal(token.value, Identifiers.While);
    }

    @Override
    public void doHandle(Token token, com.silence.vmy.compiler.Scanner remains, Stack<String> operatorStack, Stack<Tree> nodesStack) {

      handle_name_params_and_block(
          token,
          remains,
          operatorStack,
          nodesStack,
          Utils.next_two_token_should_not_be_empty_parenthesis_for_token(
              token,
              "while loop condition should not empty"
          )
      );

      // after operations, nodesStack should have at least two elements ( condition and block )
      BlockNode should_be_block = get_next_node_as_block_node_or_throw(
          nodesStack,
          () -> new ASTProcessingException("while should followed by block")
      );
      if(nodesStack.isEmpty())
        throw new ASTProcessingException("while loop has no condition");
      nodesStack.add(new WhileLoop(nodesStack.pop(), should_be_block));

    }
  }

  private static  BlockNode get_next_node_as_block_node_or_throw(
      Stack<Tree> nodes_stack,
      Supplier<ASTProcessingException> exception_provider
  ){
    final Tree should_be_block = nodes_stack.pop();
    if(!(should_be_block instanceof BlockNode))
      throw exception_provider.get();
    return (BlockNode) should_be_block;
  }

  /**
   * handle if-else syntax code block
   */
  private static class IfElHandler extends Tool {
    @Override
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<Tree> nodesStack) {
      /* if condition must be start at "if" */
      return Objects.equals(token.value, Identifiers.If);
    }

    @Override
    public void doHandle(Token token, com.silence.vmy.compiler.Scanner remains, Stack<String> operatorStack, Stack<Tree> nodesStack) {
      // if
      handle_name_params_and_block(
          token,
          remains,
          operatorStack,
          nodesStack,
          /* next two token from token */
          Utils.next_two_token_should_not_be_empty_parenthesis_for_token(
              token,
              "if condition should not empty"
          )
      );
      final ConditionNode TheIf = collect_to_condition_node(nodesStack, "");

      // elif
      List<ConditionNode> _elseIfs = new LinkedList<>();
      Token token_record;
      while(remains.hasNext() && (is_elif(remains.peek()) || Utils.isEOL(remains.peek()))){
        if(
            // ignore end of line
            // elif maybe like:
            // if .. {
            // }
            //
            //
            //
            // elif .. {
            // }
            !Utils.isEOL(token_record = remains.next())
        ){
          handle_name_params_and_block(
              token_record,
              remains,
              operatorStack,
              nodesStack,
              Utils.next_two_token_should_not_be_empty_parenthesis_for_token(
                  token_record,
                  "elif condition should not empty!"
              )
          );
          _elseIfs.add(collect_to_condition_node(nodesStack, ""));
        }
      }

      // else
      Tree _else = null;
      remove_end_of_line(remains);
      if(remains.hasNext() && is_else(remains.peek())){
        remains.next();
        remove_end_of_line(remains);
        if(!remains.hasNext())
          throw new ASTProcessingException("else has no body!");
        recall(remains.next(), remains, operatorStack, nodesStack);
        _else = nodesStack.pop();
      }
      nodesStack.add(new IfElse(TheIf, _elseIfs, _else));
    }

    private boolean is_elif(Token token){
      return Objects.equals(token.value, Identifiers.Elif);
    }

    private boolean is_else(Token token){
      return Objects.equals(token.value, Identifiers.Else);
    }

    private ConditionNode collect_to_condition_node(Stack<Tree> nodes_stack, String error_msg){
      final BlockNode block = get_next_node_as_block_node_or_throw(
          nodes_stack,
          () -> new ASTProcessingException(error_msg)
      );

      if(nodes_stack.isEmpty())
        throw new ASTProcessingException("if condition does not have condition");
      return new ConditionNode(nodes_stack.pop(), block);
    }
  }

  private static Tree mergeTwoNodes(Tree left, Tree right, String _op){
    return new BinaryOperatorNode(_op, left, right);
  }

  private static boolean operatorEquals(final String operator, final Token token){
    return Objects.equals(operator, token.value);
  }

  // a static instance
  private static TokenHandler HANDLER;

  public static TokenHandler getTokenHandler(TokenHistoryRecorder recorder){
    if(Objects.isNull(HANDLER))
      buildHandler(recorder);
    return HANDLER;
  }

  static TokenHandler getTokenHandler(){
    return getTokenHandler(null);
  }

  // when all the other handler can't handle this token throw out an ASTProcessingException
  private static class DefaultHandler extends BaseHandler {

    @Override
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<Tree> nodesStack) {
      return true;
    }

    @Override
    public void doHandle(Token token, com.silence.vmy.compiler.Scanner remains, Stack<String> operatorStack, Stack<Tree> nodesStack) {
      throw new ASTProcessingException(
          "not support token for " +
          String.format(
              "tag %d token %s",
              token.tag,
              Utils.display_newline(token.value)
          )
      );
    }
  }

  /**
   */
  private static class NewlineHandler extends BaseHandler {

    @Override
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<Tree> nodesStack) {
      return token.tag == Token.NewLine;
    }

    @Override
    public void doHandle(Token token, com.silence.vmy.compiler.Scanner remains, Stack<String> operatorStack, Stack<Tree> nodesStack) {
    }
  }

  private static class FunctionDeclarationHandler extends Tool {

    @Override
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<Tree> nodesStack) {
      return Objects.equals(token.value, Identifiers.Function);
    }

    @Override
    public void doHandle(Token token, com.silence.vmy.compiler.Scanner remains, Stack<String> operatorStack, Stack<Tree> nodesStack) {
      String name = "";
      // get function name
      if(!Utils.next_token_is(remains, Utils.OpenParenthesis())){
        name = remains.next().value;
        if(!Utils.next_token_is(remains, Utils.OpenParenthesis()))
          throw new ASTProcessingException("function declaration missing open Parenthesis");
      }
      remains.next(); // consume OpenParenthesis

      List<DeclareNode> declarations; // params
      if(Utils.next_token_is(remains, Utils.ClosingParenthesis())/* empty params */){
        remains.next(); // remove ')'
        declarations = List.of(get_return_type(remains, name, token));
      } else { /* has params */
        declarations = new ArrayList<>(4);
//        declarations.add(handle_param_declaration(token, remains, operatorStack, nodesStack));
        Token loop_end_token = Utils.ClosingParenthesis();
        Token split_token = Utils.Comma();
        while(remains.hasNext() && !Utils.next_token_is(remains, loop_end_token)){
          declarations.add(handle_param_declaration(remains.next(), remains, operatorStack, nodesStack));
          if(!Utils.next_token_is(remains, loop_end_token)){
            if(!Utils.next_token_is(remains, split_token))
              throw new ASTProcessingException("function declaration split token should be ',' : " + token);
            remains.next();// remove ','
          }
        }
        remains.next(); // remove ')'
        declarations.add(get_return_type(remains, name, split_token));
      }

      // get body
      Tree body;
      remove_end_of_line(remains);
      Utils.should_has_next_token(remains, () -> new ASTProcessingException("function declaration has no body : " + token));
      if(Utils.next_token_is(remains, Utils.OpenBrace())){
        recall(remains.next(), remains, operatorStack, nodesStack);
      }else 
        throw new ASTProcessingException("error at function declaration : " + token);
      body = nodesStack.pop();
      nodesStack.add(new FunctionNode(name, declarations, body));
    }

    private DeclareNode handle_param_declaration(Token token, com.silence.vmy.compiler.Scanner remains, Stack<String> operators, Stack<Tree> nodes){
      String name = token.value;
      Utils.should_has_and_equal(remains, Utils.Colon(), () -> new ASTProcessingException("param declaration err"));
      remains.next(); // remove ':'
      Utils.should_has_next_token(remains, () -> new ASTProcessingException("param has no type : " + token));
      return new DeclareNode("", new IdentifierNode(name), remains.next().value);
    }

    // token start at ':'
    private DeclareNode get_return_type(Scanner remains, String name, Token declaration){
      if(!Utils.next_token_is(remains, Utils.Colon())){
        throw new ASTProcessingException("function declaration has no return type declaration : (" + name + ")");
      }
      remains.next(); // remove colon

      Utils.should_has_next_token(remains, () -> new ASTProcessingException("function declaration err : " + declaration));
      return new DeclareNode("", null, remains.next().value);
    }
  }

  private static void buildHandler(TokenHistoryRecorder recorder){
    HANDLER = new HandlerBuilder()
    .next(new FunctionDeclarationHandler())
    .next(new NumberHandler())
    .next(new CallHandler())
    .next(new OperatorHandler())
    .next(new AssignmentHandler())
    .next(new DeclarationHandler())
    .next(new VariableNameHandler())
    .next(new LiteralHandler())
    .next(new BlockHandler())
    .next(new WhileHandler())
    .next(new NewlineHandler())
    .next(new IfElHandler())
    .next(new DefaultHandler())
    .build_with_each(el -> el.setTokenRecorder(recorder))
    .build();
  }

  static class HandlerBuilder {
    private List<BaseHandler> handlers = new LinkedList<>();
    private List<Consumer<BaseHandler>> before_each = new LinkedList<>();

    HandlerBuilder next(final BaseHandler next){
      handlers.add(next);
      return this;
    }

    public HandlerBuilder build_with_each(Consumer<BaseHandler> handler){
      before_each.add(handler);
      return this;
    }

    TokenHandler build(){
      if(handlers.isEmpty()) return null;
      BaseHandler head = handlers.get(0);
      do_before_each(head);
      head.setHead(head);
      BaseHandler walk = head;
      for(int i=1; i<handlers.size(); i++){
        BaseHandler current = handlers.get(i);
        do_before_each(current);
        current.setHead(head);
        walk.setNext(current);
        walk = current;
      }
      return head;
    }

    private void do_before_each(BaseHandler handler){
      for(Consumer<BaseHandler> el : before_each)
        el.accept(handler);
    }
  }

}

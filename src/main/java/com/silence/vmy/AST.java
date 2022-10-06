package com.silence.vmy;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AST {
  private AST(){}

  static interface ASTNode{

    default void accept(NodeVisitor visitor){
      throw new ASTProcessingException("your should override this method");
    }

  }
  static interface Tree{}
  static interface Evaluator{
    Object eval(Tree tree);
  }

  static class ValNode implements ASTNode {
    final Number value;
    public ValNode(Number _val){
      value = _val;
    }

    @Override
    public void accept(NodeVisitor visitor) {
    }
  }

  static class BinaryOperatorNode implements ASTNode{
    final String OP;
    ASTNode left;
    ASTNode right;
    public BinaryOperatorNode(final String _op, final ASTNode _left, final ASTNode _right){
      OP = _op;
      left = _left;
      right = _right;
    }

    @Override
    public void accept(NodeVisitor visitor) {
      visitor.visitBinaryOperator(this);
    }
  }

  private static class StringLiteral extends LiteralNode{
    private final String value;

    public StringLiteral(String value) {
      super(LiteralKind.String.ordinal());
      this.value = value;
    }

    @Override
    public Object val() {
      return value;
    }
  }

  private static class BoolLiteral extends LiteralNode{
    final Boolean value;
    public BoolLiteral(Boolean _value) {
      super(LiteralKind.Bool.ordinal());
      value = _value;
    }

    @Override
    public Object val() {
      return value;
    }
  }

  private static class NumberLiteral extends LiteralNode {
    final Number val;

    public NumberLiteral(Number _number){
      super( _number instanceof Integer ? LiteralKind.Int.ordinal() : LiteralKind.Double.ordinal());
      val = _number;
    }
    @Override
    public Object val() {
      return val;
    }
  }
  static abstract class LiteralNode implements ASTNode {
    private final int tag;
    public LiteralNode(int _tag){
      tag = _tag;
    }
    public abstract Object val();
    public int tag(){
      return tag;
    }

    @Override
    public void accept(NodeVisitor visitor) {
      visitor.visitLiteralNode(this);
    }
  }

  /**
   * a code block, like:
   * <p>let a : Int </p>
   * <p>a = 1 </p>
   * <p>print(a)</p>
   */
  static class BlockNode implements ASTNode {
    List<ASTNode> process;
    public BlockNode(List<ASTNode> _process){
      process = _process;
    }

    @Override
    public void accept(NodeVisitor visitor) {
      visitor.visitBlockNode(this);
    }
  }

  static class WhileLoop extends ConditionNode {
    public WhileLoop(ASTNode _cond, BlockNode _body){
      super(_cond, _body);
    }


    @Override
    public void accept(NodeVisitor visitor) {
      visitor.visitWhileLoop(this);
    }
  }

  public static enum LiteralKind{
    Int,
    Double,
    Char,
    Bool,
    String;
  }

  // node for assignment
  // like :
  //      let a : Type = 1
  //      a = 2
  static class AssignNode implements ASTNode {
    ASTNode variable;
    ASTNode expression;

    public AssignNode(ASTNode _variable, ASTNode expr){
      variable = _variable;
      expression = expr;
    }

    @Override
    public void accept(NodeVisitor visitor) {
      visitor.visitAssignment(this);
    }
  }

  // node for Identifier , like variable-name/function-name ...
  static class IdentifierNode implements ASTNode {
    final String value;
    public IdentifierNode(String _val){
      value = _val;
    }

    @Override
    public void accept(NodeVisitor visitor) {
      visitor.visitIdentifierNode(this);
    }
  }

  // node for Declaration, like let a : Type , val a : Type
  static class DeclareNode implements ASTNode {
    final String declare;
    final String type;
    final IdentifierNode identifier;
    public DeclareNode(String _declare, IdentifierNode _identifier, String _type){
      declare = _declare;
      identifier = _identifier;
      type =_type;
    }

    public DeclareNode(String _declare, IdentifierNode _identifier){
      this(_declare, _identifier, null);
    }

    @Override
    public void accept(NodeVisitor visitor) {
      visitor.visitDeclareNode(this);
    }
  }

  static class ConditionNode implements ASTNode {
    final ASTNode condition;
    final BlockNode body;
    public ConditionNode(ASTNode _condition, BlockNode _body){
      condition = _condition;
      body = _body;
    }
  }

  static class IfElse implements ASTNode {
    final ConditionNode TheIf;
    final List<ConditionNode> Elif;
    final ASTNode Else;
    public IfElse(ConditionNode _if, List<ConditionNode> _else_conditions, ASTNode _else){
      TheIf = Objects.requireNonNull(_if);
      Elif = _else_conditions;
      Else = _else;
    }

    @Override
    public void accept(NodeVisitor visitor) {
      visitor.visitIfElse(this);
    }
  }

  // call expression , it should be like : print("print")
  static class CallNode implements ASTNode{
    final String identifier;
    final ListExpression params;
    public CallNode(String _identifier, ListExpression _params){
      identifier = _identifier;
      params = _params;
    }

    @Override
    public void accept(NodeVisitor visitor) {
      visitor.visitCallNode(this);
    }
  }

  // function declaration
  static class FunctionNode implements ASTNode {
    final List<DeclareNode> params;
    final ASTNode body;

    public FunctionNode(List<DeclareNode> _params, ASTNode _body){
      params = _params;
      body = _body;
    }

  }

  // a list expression should be like this below:
  // a, b, c  or print(a, b, c)
  static class ListExpression implements ASTNode {
    final List<ASTNode> elements;
    public ListExpression(List<ASTNode> _els){
      elements = _els;
    }
  }

  static class VmyAST implements Tree{
    ASTNode root;
  }

  // main for support old version test
  public static VmyAST build(List<Token> tokens){
    Stack<String> operatorStack = new Stack<>();
    Stack<ASTNode> nodesStack = new Stack<>();
    Iterator<Token> tokenIterator = tokens.iterator();
    Scanner scanner = new Scanners.VmyScanner(tokens);
    TokenHandler handler = getTokenHandler();
    while(tokenIterator.hasNext()){
      handler.handle(scanner.next(), scanner, operatorStack, nodesStack);
    }
    VmyAST ast = new VmyAST();

    if(!nodesStack.isEmpty()){
      ASTNode merge = nodesStack.pop();
      while(
        !operatorStack.isEmpty() &&
        !nodesStack.isEmpty()
      ){
          final String operator = operatorStack.pop();
          final ASTNode asLeft = nodesStack.pop();
          merge = new BinaryOperatorNode(operator, asLeft, merge);
      }
      if(!nodesStack.isEmpty() || !operatorStack.isEmpty())
        throw new ASTProcessingException("expression wrong");
      ast.root = merge;
    }
    return ast;
  }

  // new version
  public static VmyAST build(Scanner scanner){
    TokenHistoryRecorder recorder = new FixedSizeCapabilityTokenRecorder(3);
    scanner.register(recorder, false);
    Stack<String> operatorStack = new Stack<>();
    Stack<ASTNode> nodesStack = new Stack<>();
    TokenHandler handler = getTokenHandler(recorder);
    while(scanner.hasNext()){
      handler.handle(scanner.next(), scanner, operatorStack, nodesStack);
    }
    VmyAST ast = new VmyAST();
    ast.root = merge_linear_nodes(nodesStack);
    return ast;
  }

  private static ASTNode merge_linear_nodes(List<ASTNode> nodes){
//    return new Process
    return new BlockNode(nodes);
  }

  static interface TokenHandler{
    void handle(Token token, Scanner remains, Stack<String> operatorStack, Stack<ASTNode> nodesStack);
  }


  private static 
  abstract class BaseHandler 
    implements TokenHandler, 
    Utils.Recursive, 
    TokenHistoryRecorderGetter
  {
    private BaseHandler next;

    private BaseHandler head;

    private TokenHistoryRecorder tokenHistoryRecorder;

    public void setNext(final BaseHandler _next){
      next = _next;
    }

    public void setHead(BaseHandler _head){
      head = _head;
    }

    final protected void recall(
      Token token, 
      Scanner remains, 
      Stack<String> operatorStack, 
      Stack<ASTNode> nodesStack
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
      Scanner remains, 
      Stack<String> operatorStack, 
      Stack<ASTNode> nodesStack
    ){
      try {
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
      Stack<ASTNode> nodesStack
    );

    public abstract void doHandle(
      Token token, 
      Scanner remains, 
      Stack<String> operatorStack, 
      Stack<ASTNode> nodesStack
    );

  }

  private static class NumberHandler extends BaseHandler{

    @Override
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {
      return token.tag == Token.INT_V || token.tag == Token.DOUBLE_V;
    }

    @Override
    public void doHandle(
      Token token, 
      Scanner remains, 
      Stack<String> operatorStack, 
      Stack<ASTNode> nodesStack
    ) {

      nodesStack.add(
        token.tag == Token.DOUBLE_V ? 
        new ValNode( Double.parseDouble(token.value) ) : 
        new ValNode( Integer.parseInt(token.value) )
      );

    }

  }

  private static class OperatorHandler extends Tool{

    @Override
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {
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
    public void doHandle(Token token, Scanner remains, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {

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
            ASTNode left = nodesStack.pop();
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

        ASTNode right = nodesStack.pop();
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

      } else if(/* a call like : print(1) */token.tag == Token.BuiltinCall){

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
        LinkedList<ASTNode> params = new LinkedList<>();
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

      } else if(operatorEquals(Identifiers.MULTI, token) || operatorEquals(Identifiers.DIVIDE, token) ){

        if(!remains.hasNext())
          throw new ASTProcessingException("*(multiply) doesn't have right side");
        if(nodesStack.isEmpty())
          throw new ASTProcessingException("*(multiply) left side not exists");
        ASTNode left = nodesStack.pop();
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

  // represent an empty node
  private static class EmptyNode implements ASTNode{}

  private static abstract class Tool extends BaseHandler {

    // a default option for remove end-op
    protected void travel_back_build(
        Token token,
        Scanner remains,
        Stack<String> operation_stack,
        Stack<ASTNode> nodes_stack,
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
        Scanner remains,
        Stack<String> operation_stack,
        Stack<ASTNode> nodes_stack,
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
    protected ASTNode merge_back(
        Stack<String> operation_stack,
        Stack<ASTNode> nodes_stack,
        BiPredicate<Stack<ASTNode>, Stack<String>> end_condition
    ){
      if(nodes_stack.isEmpty())
        throw new ASTProcessingException("merge node error , because node_stack isEmpty");

      ASTNode merge_node = nodes_stack.pop();

      while(
          !operation_stack.isEmpty() &&
          !nodes_stack.isEmpty() &&
          !end_condition.test(nodes_stack, operation_stack)
      ){
        final String operator = operation_stack.pop();
        final ASTNode asLeft = nodes_stack.pop();
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
        Scanner remains,
        Stack<String> operator_stack,
        Stack<ASTNode> nodes_stack,
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
    protected void remove_end_of_line(Scanner remains){
      while(remains.hasNext() && Utils.isEOL(remains.peek()))
        remains.next();
    }
  }


  // handle name like variable name
  private static class VariableNameHandler extends BaseHandler{
    @Override
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {
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
      Scanner remains, 
      Stack<String> operatorStack, 
      Stack<ASTNode> nodesStack
    ) {

      nodesStack.add(new IdentifierNode(token.value));

    }
  }

  private static class LiteralHandler extends Tool{
    @Override
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {
      return token.tag == Token.Literal;
    }

    @Override
    public void doHandle(Token token, Scanner remains, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {

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
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {
      return token.tag == Token.Assignment;
    }

    @Override
    public void doHandle(Token token, Scanner remains, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {

      // 1 get the variable name or a declaration
      ASTNode variable;
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
      ASTNode the_value = nodesStack.pop();
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
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {
      return token.tag == Token.Declaration;
    }

    @Override
    public void doHandle(Token token, Scanner remains, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {

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
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {
      return Utils.equal(token.value, Identifiers.OpenBrace);
    }

    @Override
    public void doHandle(Token token, Scanner remains, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {

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

      LinkedList<ASTNode> params = new LinkedList<>();
      // add all line node to list
      ASTNode temp_node;
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
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {
      return Utils.equal(token.value, Identifiers.While);
    }

    @Override
    public void doHandle(Token token, Scanner remains, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {

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
      Stack<ASTNode> nodes_stack,
      Supplier<ASTProcessingException> exception_provider
  ){
    final ASTNode should_be_block = nodes_stack.pop();
    if(!(should_be_block instanceof BlockNode))
      throw exception_provider.get();
    return (BlockNode) should_be_block;
  }

  /**
   * handle if-else syntax code block
   */
  private static class IfElHandler extends Tool {
    @Override
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {
      /* if condition must be start at "if" */
      return Objects.equals(token.value, Identifiers.If);
    }

    @Override
    public void doHandle(Token token, Scanner remains, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {
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
      ASTNode _else = null;
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

    private ConditionNode collect_to_condition_node(Stack<ASTNode> nodes_stack, String error_msg){
      final BlockNode block = get_next_node_as_block_node_or_throw(
          nodes_stack,
          () -> new ASTProcessingException(error_msg)
      );

      if(nodes_stack.isEmpty())
        throw new ASTProcessingException("if condition does not have condition");
      return new ConditionNode(nodes_stack.pop(), block);
    }
  }

  private static ASTNode mergeTwoNodes(ASTNode left, ASTNode right, String _op){
    return new BinaryOperatorNode(_op, left, right);
  }

  private static boolean operatorEquals(final String operator, final Token token){
    return Objects.equals(operator, token.value);
  }

  // a static instance
  private static TokenHandler HANDLER;

  static TokenHandler getTokenHandler(TokenHistoryRecorder recorder){
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
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {
      return true;
    }

    @Override
    public void doHandle(Token token, Scanner remains, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {
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
    public boolean canHandle(Token token, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {
      return token.tag == Token.NewLine;
    }

    @Override
    public void doHandle(Token token, Scanner remains, Stack<String> operatorStack, Stack<ASTNode> nodesStack) {
    }
  }

  private static void buildHandler(TokenHistoryRecorder recorder){
    HANDLER = new HandlerBuilder()
    .next(new NumberHandler())
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

  public static Evaluator defaultTreeEvaluator() {
    return Evaluator;
  }

  private static VmyTreeEvaluator Evaluator = new VmyTreeEvaluator();

  private static class VmyTreeEvaluator implements Evaluator{
    private Global  _g = Global.getInstance();

    @Override
    public Object eval(Tree tree) {
      if(tree instanceof VmyAST ast){
        return evalsub(ast.root);
      }else
        throw new EvaluatException("unrecognized AST");
    }

    Object evalsub(ASTNode node){
      if(node instanceof ValNode val){
        return val.value;
      }else if(node instanceof BinaryOperatorNode common){
        Object left = evalsub(common.left);
        Object right  = evalsub(common.right);
        if(Objects.isNull(right) || Objects.isNull(left))
          throw new EvaluatException(common.OP + " can't handle null object");
        BinaryOps op = BinaryOps.OpsMapper.get(common.OP);
        if(Objects.isNull(op))
          throw new EvaluatException("op(" + common.OP + ") not support!");
        return Objects.nonNull(op) ? op.apply(getValue(left), getValue( right )) : null;
      }else if(node instanceof AssignNode assignment){
        String variable_name = (String) evalsub(assignment.variable);
        Object value = getValue( evalsub(assignment.expression) );
        if(value instanceof LiteralNode string_literal)
          value = string_literal.val();
        findAndPut(variable_name, value);
        return value;
      } else if(node instanceof DeclareNode declaration){
        _g.put(declaration.identifier.value, null);
        return declaration.identifier.value;
      }else if(node instanceof IdentifierNode identifier){
        return identifier.value;
      }else if(node instanceof LiteralNode literal){
        return literal;
      } else if(node instanceof BlockNode block){
        for(ASTNode el : block.process){
          evalsub(el);
        }
        return null;
      } else
        throw new EvaluatException("unrecognizable AST node : " + node.getClass().getName());
    }

    Object getValue(Object obj){
      if(obj instanceof String obj_name) {
        if(!_g.exists(obj_name))
          throw new EvaluatException(String.format("variable (%s) not exists", obj_name));
        return _g.get(obj_name);
      }
      return obj;
    }

    /**
     * @param _name
     * @param _value
     */
    void findAndPut(String _name, Object _value){
      if(!_g.exists(_name))
        throw new EvaluatException(_name + " haven't declared!");
      _g.put(_name, _value);
    }

  }

  public static Evaluator variableStoreTreeEvaluator(){
    return VSTEvaluator;
  }

  public static Evaluator evaluator(boolean create){
    return create ? new VariableStoreTreeEvaluator() : variableStoreTreeEvaluator();
  }

  private static VariableStoreTreeEvaluator VSTEvaluator = new VariableStoreTreeEvaluator();

  private static class VariableStoreTreeEvaluator implements Evaluator{
    private Global  _g = Global.getInstance();

    @Override
    public Object eval(Tree tree) {
      if(tree instanceof VmyAST ast){
        return eval_sub(ast.root);
      }else
        throw new EvaluatException("unrecognized AST");
    }

    /**
     * evaluate each node of the tree
     * @param node {@link ASTNode}
     * @return the node evaluating result , like
     */
    Object eval_sub(ASTNode node){

      if(node instanceof ValNode val){
        return val.value;
      }else if(node instanceof BlockNode block){

        List<ASTNode> nodes = block.process;
        for(ASTNode sub : nodes){
          eval_sub(sub);
        }
        return null;

      } else if(node instanceof BinaryOperatorNode common){

        return binary_op_call(
            common.OP ,
            eval_sub(common.left),
            eval_sub(common.right)
        );

      }else if(node instanceof AssignNode assignment){

        return handle_assignment_node(assignment);

      } else if(node instanceof DeclareNode declaration){
        return Utils.variable_with_name(
            declaration.identifier.value,
            Runtime.declare_variable(
                _g,
                declaration.identifier.value,
                Utils.to_type(declaration.type),
                Utils.is_mutable(declaration.declare)
            )
        );
      }else if(node instanceof IdentifierNode identifier){

        try {
          return get_variable(identifier.value);
        }catch (Exception e){
          Utils.error(e.getMessage());
          return null;
        }

      }else if(node instanceof LiteralNode literal){
        return literal.val();
      } else if(node instanceof CallNode call){
        return do_call(call);
      } else if( node instanceof WhileLoop while_loop){
        while((boolean)eval_sub(while_loop.condition)){
          eval_sub(while_loop.body);
        }
        return null;
      } else if(node instanceof IfElse ifElse){
        do_evaluate_if_else(ifElse);
        return null;
      } else
        throw new EvaluatException("unrecognizable AST node");
    }

    // compare type
    // check if variable can be assigned
    Object handle_assignment_node(AssignNode assignment){
      Object expression = eval_sub(assignment.expression);
      VmyType expression_type = Utils.get_obj_type(expression);

      Object expression_value = get_value(expression);
      if(assignment.variable instanceof IdentifierNode identifier){
        try {
          Runtime.VariableWithName identifier_variable = get_variable(identifier.value);
          can_assign(identifier_variable, expression);
          assign_to(identifier_variable.name(), identifier_variable, expression_value);
        }catch (Exception e){
          Utils.error(e.getMessage());
        }
      }else if(assignment.variable instanceof  DeclareNode declaration){

        final VmyType declaration_type = Objects.isNull(declaration.type) ? expression_type : Utils.to_type(declaration.type);
        can_assign(declaration_type, expression_type);
        assign_to(
            declaration.identifier.value,
            Runtime.declare_variable(
                _g,
                declaration.identifier.value,
                declaration_type,
                Utils.is_mutable(declaration.declare)
            ),
            expression_value
        );
      }
      return expression_value;
    }

    /**
     * handle the binary operation like : 1 + 2, 2 * 4
     * @param op operation
     * @param left operation left side
     * @param right operation right side
     * @return call result , like : 1 + 2 -> 3
     */
    Object binary_op_call(String op, Object left , Object right){
      if(Objects.isNull(right) || Objects.isNull(left))
        throw new EvaluatException(op + " can't handle null object");
      BinaryOps b_op = BinaryOps.OpsMapper.get(op);
      if(Objects.isNull(b_op))
        throw new EvaluatException("op(" + op + ") not support!");
      return Objects.nonNull(b_op) ? b_op.apply(get_value(left), get_value( right )) : null;
    }

    /**
     * if type of {@code expression_type} can be assigned to type of {@code variable_type}
     * @param variable_type variable type
     * @param expression_type expression type
     * @return true else throw {@link ASTProcessingException}
     */
    boolean can_assign(VmyType variable_type, VmyType expression_type){
      if(!Utils.equal(variable_type, expression_type))
        throw new ASTProcessingException("type " + expression_type + " can not be assigned to type " + variable_type);
      return true;
    }

    /**
     * check the declaration , if it's const variable , it will not be assigned, then check if the type is match
     * @param variable {@link Runtime.VariableWithName}
     * @param value assigned value
     */
    void can_assign(Runtime.VariableWithName variable, Object value){
      if(!variable.mutable())
        throw new EvaluatException("const variable (let) can't be assigned : " + variable.name());
      can_assign(variable.getType(), Utils.get_obj_type(value));
    }

    /**
     * evaluate the code block if-else
     * @param if_else {@link IfElse}
     */
    void do_evaluate_if_else(IfElse if_else){
      ConditionNode the_if = if_else.TheIf;
      if((boolean)eval_sub(the_if.condition)){
        eval_sub(the_if.body);
      }else if(!eval_elif(if_else.Elif) && Objects.nonNull(if_else.Else)){
        eval_sub(if_else.Else);
      }
    }

    boolean eval_elif(List<ConditionNode> _ifEls){
      for(ConditionNode _el : _ifEls){
        if((boolean) eval_sub(_el.condition)){
          eval_sub(_el.body);
          return true;
        }
      }
      return false;
    }

    /**
     * handle a call expression like:
     *
     * print(1, "string")
     * @param call_node
     * @return
     */
    Object do_call(CallNode call_node){
      // get params and check if the type is matched
      // get the called function type
      return FunctionSupport.call(
          call_node.identifier,
          call_node.params.elements.stream()
              .map(param -> get_value(eval_sub(param)))
              .toList()
      );
    }

    Runtime.VariableWithName get_variable(String name){
      Runtime.Variable variable = _g.local(name);
      if(Objects.isNull(variable))
        throw new EvaluatException("variable " + name + " haven't declared!");
      return Utils.variable_with_name(name, variable);
    }

    // assign value to variable
    void assign_to(String variable_name, Runtime.Variable variable, Object value){
      _g.put(variable_name, variable, value);
    }


    /**
     * get the value of an object
     *
     * classified to 2 type
     *
     * 1. Variable type : get the value that the variable point at
     *
     * 2. it's a value : return
     * @param obj
     * @return
     */
    Object get_value(Object obj){
      if(obj instanceof Runtime.VariableWithName variable) {
        return Runtime.get_value(variable.name(), _g);
      }
      return obj;
    }

  }
}

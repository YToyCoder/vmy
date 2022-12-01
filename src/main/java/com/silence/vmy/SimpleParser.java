package com.silence.vmy;

import java.util.Stack;

public class SimpleParser implements Parser{

  private final TokenHistoryRecorder token_recorder;
  private final Scanner scanner;
  private final AST.TokenHandler token_handler;

  private SimpleParser(Scanner _scanner, AST.TokenHandler _token_handler, int _token_record_size){
    token_handler = _token_handler;
    scanner = _scanner;
    token_recorder = new FixedSizeCapabilityTokenRecorder(_token_record_size);
  }

  private SimpleParser(Scanner _scanner, AST.TokenHandler _token_handler, TokenHistoryRecorder _recorder){
    scanner = _scanner;
    token_handler = _token_handler;
    token_recorder = _recorder;
  }

  public static Parser create(Scanner scanner){
    TokenHistoryRecorder recorder = new FixedSizeCapabilityTokenRecorder(3);
    scanner.register(recorder, false);
    return new SimpleParser(scanner, AST.getTokenHandler(recorder), recorder);
  }

  @Override
  public AST.Tree parse() {
    scanner.register(token_recorder, false);
    Stack<String> operators = new Stack<>();
    Stack<AST.ASTNode> nodes = new Stack<>();
    while(scanner.hasNext()){
      token_handler.handle(scanner.next(), scanner, operators, nodes);
    }
    AST.VmyAST ast = new AST.VmyAST();
    ast.root = switch (nodes.size()) {
      case 0 -> null;
      case 1 -> nodes.get(0);
      default -> try_merge(operators, nodes);
    };
    return ast;
  }

  private AST.ASTNode try_merge(Stack<String> operators, Stack<AST.ASTNode> nodes){
    if(operators.isEmpty())
      return new AST.BlockNode(nodes);
    AST.ASTNode node = nodes.pop();
    while(!operators.isEmpty() && !nodes.isEmpty()){
      node = new AST.BinaryOperatorNode(operators.pop(), nodes.pop(), node);
    }
    return node;
  }
}

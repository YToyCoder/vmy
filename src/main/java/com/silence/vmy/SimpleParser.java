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

  public static Parser instance(Scanner _scanner, AST.TokenHandler _tk_handler, int token_record_size){
    return new SimpleParser(_scanner, _tk_handler, token_record_size);
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
      default -> new AST.BlockNode(nodes);
    };
    return ast;
  }
}

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

  public static Parser instance(Scanner _scanner, AST.TokenHandler _tk_handler, int token_record_size){
    return new SimpleParser(_scanner, _tk_handler, token_record_size);
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
    ast.root =new AST.BlockNode(nodes);
    return ast;
  }
}

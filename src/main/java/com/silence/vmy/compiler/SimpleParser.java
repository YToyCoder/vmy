package com.silence.vmy.compiler;

import com.silence.vmy.compiler.oldIR.Scanner;
import com.silence.vmy.compiler.oldIR.BinaryOperatorNode;
import com.silence.vmy.compiler.oldIR.BlockNode;
import com.silence.vmy.compiler.tree.Root;
import com.silence.vmy.compiler.tree.Tree;

import java.util.Stack;

public class SimpleParser implements Parser {

  private final TokenHistoryRecorder token_recorder;
  private final Scanner scanner;

  private SimpleParser(Scanner _scanner, TokenHistoryRecorder _recorder){
    scanner = _scanner;
    token_recorder = _recorder;
  }

  public static Parser create(Scanner scanner){
    TokenHistoryRecorder recorder = new FixedSizeCapabilityTokenRecorder(3);
    scanner.register(recorder, false);
    return new SimpleParser(scanner, recorder);
  }

  @Override
  public Root parse() { return null;}

  private Tree try_merge(Stack<String> operators, Stack<Tree> nodes){
    if(operators.isEmpty())
      return new BlockNode(nodes);
    Tree node = nodes.pop();
    while(!operators.isEmpty() && !nodes.isEmpty()){
      node = new BinaryOperatorNode(operators.pop(), nodes.pop(), node);
    }
    return node;
  }
}

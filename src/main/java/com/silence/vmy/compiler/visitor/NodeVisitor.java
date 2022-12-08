package com.silence.vmy.compiler.visitor;

import com.silence.vmy.compiler.deprecated.*;

public interface NodeVisitor {
  /**
   * visit {@link BinaryOperatorNode} , like 1 + 2
   * @param node
   */
  void visitBinaryOperator(BinaryOperatorNode node);

  /**
   * visit {@link BlockNode}, code block
   * @param node
   */
  void visitBlockNode(BlockNode node);

  /**
   * visit {@link WhileLoop}, a code block start with 'if'
   * @param node
   */
  void visitWhileLoop(WhileLoop node);

  /**
   * visit {@link AssignNode}, an assignment code
   * @param node
   */
  void visitAssignment(AssignNode node);

  /**
   * visit {@link DeclareNode}, a declaration which start with 'let' or 'val'
   * @param node
   */
  void visitDeclareNode(DeclareNode node);

  /**
   * visit {@link IdentifierNode}, a identifier like variable name, function name
   * @param node
   */
  void visitIdentifierNode(IdentifierNode node);

  /**
   * visit {@link LiteralNode}, a literal like string, boolean, number
   * @param node
   */
  void visitLiteralNode(LiteralNode node);

  /**
   * visit {@link CallNode}, a call expression like, 'print("hello")'
   * @param node
   */
  void visitCallNode(CallNode node);

  /**
   * visit {@link IfElse}, a condition start with 'if'
   * @param node
   */
  void visitIfElse(IfElse node);

  /**
   * visit {@link FunctionNode}, a function declaration
   * @param node
   */
  void visitFunction(FunctionNode node);

  void visitReturn(Return node);

  void visitEmpty(EmptyNode node);
}

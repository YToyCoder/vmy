package com.silence.vmy;

public interface NodeVisitor {
  /**
   * visit {@link com.silence.vmy.AST.BinaryOperatorNode} , like 1 + 2
   * @param node
   */
  void visitBinaryOperator(AST.BinaryOperatorNode node);

  /**
   * visit {@link com.silence.vmy.AST.BlockNode}, code block
   * @param node
   */
  void visitBlockNode(AST.BlockNode node);

  /**
   * visit {@link com.silence.vmy.AST.WhileLoop}, a code block start with 'if'
   * @param node
   */
  void visitWhileLoop(AST.WhileLoop node);

  /**
   * visit {@link com.silence.vmy.AST.AssignNode}, an assignment code
   * @param node
   */
  void visitAssignment(AST.AssignNode node);

  /**
   * visit {@link com.silence.vmy.AST.DeclareNode}, a declaration which start with 'let' or 'val'
   * @param node
   */
  void visitDeclareNode(AST.DeclareNode node);

  /**
   * visit {@link com.silence.vmy.AST.IdentifierNode}, a identifier like variable name, function name
   * @param node
   */
  void visitIdentifierNode(AST.IdentifierNode node);

  /**
   * visit {@link com.silence.vmy.AST.LiteralNode}, a literal like string, boolean, number
   * @param node
   */
  void visitLiteralNode(AST.LiteralNode node);

  /**
   * visit {@link com.silence.vmy.AST.CallNode}, a call expression like, 'print("hello")'
   * @param node
   */
  void visitCallNode(AST.CallNode node);

  /**
   * visit {@link com.silence.vmy.AST.IfElse}, a condition start with 'if'
   * @param node
   */
  void visitIfElse(AST.IfElse node);

  /**
   * visit {@link com.silence.vmy.AST.FunctionNode}, a function declaration
   * @param node
   */
  void visitFunction(AST.FunctionNode node);
}

package com.silence.vmy.compiler.tree;

public interface TreeVisitor<R,T> {
  R visitLiteral(LiteralExpression expression, T payload);
  R visitUnary(Unary expression, T payload);
  R visitBlock(BlockStatement statement, T payload);
  R visitBinary(BinaryOperateExpression expression, T payload);
  R visitVariableDecl(VariableDecl expression, T payload);
  R visitAssignment(AssignmentExpression expression, T payload);
  R visitFunctionDecl(FunctionDecl function, T payload);
  R visitRoot(Root root, T payload);
  R visitListExpr(ListExpr expr, T payload);
  R visitReturnExpr(ReturnExpr expr, T payload);
}

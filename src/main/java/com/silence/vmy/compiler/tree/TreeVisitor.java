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
  <E extends Expression> R visitListExpr(ListExpr<E> expr, T payload);
  R visitReturnExpr(ReturnExpr expr, T payload);
  R visitTypeExpr(TypeExpr expr, T payload);
  R visitCallExpr(CallExpr expr, T payload);
  R visitIdExpr(IdExpr expr, T payload);
  R visitIfStatement(IfStatement statement, T payload);
  R visitArr(ArrExpression arr, T t);
  R visitForStatement(ForStatement forStatement, T t);
  R visitVmyObject(VmyObject forStatement, T t);
  R visitImport(ImportState state, T t);
  R visitExport(ExportState state, T t);
}

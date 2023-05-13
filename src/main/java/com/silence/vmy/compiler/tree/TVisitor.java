package com.silence.vmy.compiler.tree;

public abstract class TVisitor<T> {

  public boolean enterIdExpr(IdExpr expr, T t) { return enterDefault(expr, t); }
  public Tree leaveIdExpr(IdExpr expr, T t) { return leaveDefault(expr, t); }

  public boolean enterUnary(Unary unary, T t) { return enterDefault(unary, t); }
  public Tree leaveUnary(Unary unary, T t) { return leaveDefault(unary, t); }

  public boolean enterBlock(BlockStatement block, T t) { return enterDefault(block, t); }
  public Tree leaveBlock(BlockStatement block, T t) { return leaveDefault(block, t); }

  public boolean enterBinary(BinaryOperateExpression binaryOperation, T t) { return enterDefault(binaryOperation, t); }
  public Tree leaveBinary(BinaryOperateExpression binaryOperation, T t) { return leaveDefault(binaryOperation, t); }

  public boolean enterVariableDecl(VariableDecl variableDecl, T t) { return enterDefault(variableDecl, t); }
  public Tree leaveVariableDecl(VariableDecl variableDecl, T t) { return leaveDefault(variableDecl, t); }

  public boolean enterAssignment(AssignmentExpression assignment, T t) { return enterDefault(assignment, t); }
  public Tree leaveAssignment(AssignmentExpression assignment, T t) { return leaveDefault(assignment, t); }

  public boolean enterFunctionDecl(FunctionDecl functionDecl, T t) { return enterDefault(functionDecl, t); }
  public Tree leaveFunctionDecl(FunctionDecl functionDecl, T t) { return leaveDefault(functionDecl, t); }

  public boolean enterRoot(Root root, T t) { return enterDefault(root, t); }
  public Tree leaveRoot(Root root, T t) { return leaveDefault(root, t); }

  public <E extends Expression> boolean enterListExpr(ListExpr<E> expr, T t) { return enterDefault(expr, t); }
  public <E extends Expression> Tree leaveListExpr(ListExpr<E> expr, T t) { return leaveDefault(expr, t); }

  public boolean enterReturn(ReturnExpr ret, T t) { return enterDefault(ret, t); }
  public Tree leaveReturn(ReturnExpr ret, T t) { return leaveDefault(ret, t); }

  public boolean enterTypeExpr(TypeExpr typeExpr, T t) { return enterDefault(typeExpr, t); }
  public Tree leaveTypeExpr(TypeExpr typeExpr, T t) { return leaveDefault(typeExpr, t); }

  public boolean enterCallExpr(CallExpr call, T t) { return enterDefault(call, t); }
  public Tree leaveCallExpr(CallExpr call, T t) { return leaveDefault(call, t); }

  public boolean enterLiteral(LiteralExpression literal, T t) { return enterDefault(literal, t); }
  public Tree leaveLiteral(LiteralExpression literal, T t) { return leaveDefault(literal, t); }

  public boolean enterIfStatement(IfStatement ifStatement, T t) { return enterDefault(ifStatement, t); }
  public Tree leaveIfStatement(IfStatement ifStatement, T t) { return leaveDefault(ifStatement, t); }

  public boolean enterArrExpression(ArrExpression arr, T t) { return enterDefault(arr, t); }
  public Tree leaveArrExpression(ArrExpression arr, T t) { return leaveDefault(arr, t); }

  public boolean enterForStatement(ForStatement state, T t) { return enterDefault(state, t); }
  public Tree leaveForStatement(ForStatement state, T t) { return leaveDefault(state, t); }

  public boolean enterVmyObject(VmyObject obj, T t) { return enterDefault(obj, t); }
  public Tree leaveVmyObject(VmyObject obj, T t) { return leaveDefault(obj, t); }

  public boolean enterImport(ImportState state, T t) { return enterDefault(state, t); }
  public Tree leaveImport(ImportState state, T t) { return leaveDefault(state, t); }

  public boolean enterExport(ExportState state, T t) { return enterDefault(state, t); }
  public Tree leaveExport(ExportState state, T t) { return leaveDefault(state, t); }

  protected boolean enterDefault(Tree tree, T t) { return true; }
  protected Tree leaveDefault(Tree tree, T t) { return tree; }

}

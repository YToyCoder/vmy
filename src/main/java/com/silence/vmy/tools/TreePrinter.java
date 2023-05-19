package com.silence.vmy.tools;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.silence.vmy.compiler.tree.*;

public class TreePrinter implements TreeVisitor<String,Integer>{

  public String tree_as_string(Tree tree){
    if(Objects.isNull(tree)) return "Empty-Tree";
    return tree.accept(this, 0);
  }

  @Override
  public String visitLiteral(LiteralExpression expression, Integer deep) {
    return expression.toString();
  }

  @Override
  public String visitUnary(Unary expression, Integer deep) {
    return "%s<|%s".formatted(expression.tag(), expression.accept(this, deep));
  }

  @Override
  public String visitBlock(BlockStatement statement, Integer deep) {
    String prefix = " ".repeat(deep);
    String line_prefix = " ".repeat(deep + 1);
    StringBuilder sb = new StringBuilder(prefix + "{\n");
    for(var line : statement.exprs()){
      sb.append(line_prefix + line.accept(this, deep + 1) + "\n");
    }
    sb.append(prefix + "}\n");
    return sb.toString();
  }

  @Override
  public String visitBinary(BinaryOperateExpression expression, Integer deep) {
    return expression.left().accept(this, deep) + "[%s]".formatted(expression.tag()) + expression.right().accept(this, deep);
  }

  @Override
  public String visitVariableDecl(VariableDecl expression, Integer deep) {
    return expression.toString();
  }

  @Override
  public String visitAssignment(AssignmentExpression expression, Integer deep) {
    return expression.left().accept(this, deep) + " = "+ expression.right().accept(this, deep);
  }

  @Override
  public String visitFunctionDecl(FunctionDecl function, Integer deep) {
    StringBuilder sb = new StringBuilder("Fn(" + function.name() + "):");
    List<VariableDecl> params = function.params();
    sb.append("(");
    for(int i=0; i<params.size() - 1; i++){
      VariableDecl decl = params.get(0);
      sb.append(decl + ",");
    }
    if(params.size() > 0) {
      sb.append(params.get(params.size() - 1));
    }
    sb.append(") => " + (Objects.isNull(function.ret()) ? "?" : function.ret().typeId() ) + "\n");
    return sb.toString() + function.body().accept(this, deep);
  }

  @Override
  public String visitRoot(Root root, Integer deep) {
    if(Objects.isNull(root)) return "";
    StringBuilder sb = new StringBuilder(root.file_name() + "\n");
    if(Objects.nonNull(root.body())){
      sb.append(root.body().accept(this, deep));
    } 
    return sb.toString();
  }

  @Override
  public <E extends Expression> String visitListExpr(ListExpr<E> expr, Integer deep) {
    return "ListExpr-not-support";
  }

  @Override
  public String visitReturnExpr(ReturnExpr expr, Integer deep) {
    return "return " + expr.body().accept(this, deep);
  }

  @Override
  public String visitTypeExpr(TypeExpr expr, Integer deep) {
    return expr.typeId();
  }

  @Override
  public String visitCallExpr(CallExpr expr, Integer deep) {
    String callId = expr.callId();
    ListExpr<? extends Tree> params = expr.params();
    if(Objects.isNull(params) || Objects.isNull(params.body()) || params.body().isEmpty()){
      return callId + "()";
    }
    StringBuilder sb = new StringBuilder(callId + "(");
    for(Tree elem : params.body()){
      sb.append(elem.accept(this, deep) + ", ");
    }
    return sb.append(")").toString();
  }

  @Override
  public String visitIdExpr(IdExpr expr, Integer deep) {
    return "Id(%s)".formatted(expr.name());
  }

  @Override
  public String visitIfStatement(IfStatement statement, Integer deep) {
    StringBuffer sb = new StringBuffer();
    sb.append(visitCond(statement.ifStatement(), deep));
    List<ConditionStatement> elif = statement.elif();
    if(Objects.nonNull(elif) && !elif.isEmpty()) {
      for(ConditionStatement cond : elif){
        sb.append(" ".repeat(deep) + visitCond(cond, deep));
      }
    }
    if(Objects.nonNull(statement.el())){
      sb.append(" ".repeat(deep) + "Else \n");
      sb.append(statement.el().accept(this, deep));
    }
    return sb.toString();
  }

  private String visitCond(ConditionStatement cond, Integer deep) {
    StringBuilder sb = new StringBuilder("%s %s".formatted( cond.tag() ,cond.condition().accept(this, deep)));
    sb.append("\n").append(cond.block().accept(this, deep));
    return sb.toString();
  }

  @Override
  public String visitArr(ArrExpression arr, Integer t) {
    List<Expression> elems = arr.elements();
    if(elems.isEmpty()) return "[]";
    StringBuilder sb = new StringBuilder("[\n");
    for(Expression el : elems){
      sb.append(" ".repeat(t) + el.accept(this, t) + "\n");
    }
    sb.append(" ".repeat(t) + "]\n");
    return sb.toString();
  }

  @Override
  public String visitForStatement(ForStatement forStatement, Integer deep) {
    StringBuilder sb = new StringBuilder("For ");
    forStatement.heads().forEach((idexp) -> {
      sb.append(idexp.accept(this, deep) + ", ");
    });
    sb.append(forStatement.arrId().accept(this, deep) + "\n");
    return sb.append(forStatement.body().accept(this, deep)).toString(); 
  }

  @Override
  public String visitVmyObject(VmyObject obj, Integer t) {
    Map<String,Expression> props = obj.properties();
    if(props.isEmpty()) return "{}";
    StringBuilder sb = new StringBuilder("{\n");
    props.entrySet().stream().forEach((entry) -> {
      sb.append(" ".repeat(t) + "%s => %s".formatted(entry.getKey(), entry.getValue().accept(this, t)) + ",\n");
    });
    sb.append(" ".repeat(t) + "}\n");
    return sb.toString(); 
  }

  @Override
  public String visitImport(ImportState state, Integer t) {
    return "Import-Not-PrinT";
  }

  @Override
  public String visitExport(ExportState state, Integer t) {
    return "Export-Not-PrinT";
  }
}

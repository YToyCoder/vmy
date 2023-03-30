package com.silence.vmy.compiler.transform;

import com.silence.vmy.compiler.AST;
import com.silence.vmy.compiler.CompilationException;
import com.silence.vmy.compiler.Identifiers;
import com.silence.vmy.compiler.Modifiers;
import com.silence.vmy.compiler.oldIR.*;
import com.silence.vmy.compiler.tree.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;

import static com.silence.vmy.compiler.tree.Tree.Tag.Sub;

public abstract class IrTransforms {

  public static class Convert2OldIR implements TreeVisitor<Tree, Object> {
    @Override
    public Tree visitLiteral(LiteralExpression expression, Object payload) {
      if(expression.isString())
        return new StringLiteral((String) expression.literal());
      if(expression.isNumber())
        return new NumberLiteral(expression.isInt() ? expression.getInteger() : expression.getDouble());
      if(expression.isBoolean())
        return new BoolLiteral(expression.getBoolean());
      throw new CompilationException();
    }

    @Override
    public Tree visitUnary(Unary unary, Object payload) {
      Tree body = unary.body();
      int flag = 1; //
      ToIntFunction<Unary> getSign = (Unary tree) ->
          tree.tag() == Sub ? -1/* - */ :  1; // + and others
      while(body instanceof Unary _unary){
        flag *= getSign.applyAsInt(unary);
        unary = _unary;
      }
      flag *= getSign.applyAsInt(unary);
      if(unary.body() instanceof LiteralExpression literal){
        if(!literal.isNumber())
          return null;
        return new NumberLiteral((literal.isInt() ? literal.getInteger() : literal.getDouble()) * flag);
      }
      return new NumberLiteral(null);
    }

    @Override
    public Tree visitBlock(BlockStatement statement, Object payload) {
      return new BlockNode(listMap(statement.exprs(), payload));
    }

    @Override
    public Tree visitBinary(BinaryOperateExpression expression, Object payload) {
      return new BinaryOperatorNode(
          Tree.opTag2String(expression.tag()),
          expression.left().accept(this, payload),
          expression.right().accept(this, payload));
    }

    @Override
    public Tree visitVariableDecl(VariableDecl expression, Object payload) {
      return new DeclareNode(
          expression.modifiers().is(Modifiers.CVariableConst)? Identifiers.ConstDeclaration : Identifiers.VarDeclaration,
          new IdentifierNode(expression.name()));
    }

    @Override
    public Tree visitAssignment(AssignmentExpression expression, Object payload) {
      return new AssignNode(expression.left().accept(this, payload), expression.right().accept(this, payload));
    }

    @Override
    public Tree visitFunctionDecl(FunctionDecl function, Object payload) {
      // todo
      List<VariableDecl> params = new ArrayList<>(function.params());
      params.add(new VariableDecl("", Modifiers.Const, function.ret(), function.ret().position()));
      return new FunctionNode(
          function.name(),
          params
          .stream()
          .map(el ->
              new DeclareNode(
                Identifiers.ConstDeclaration,
                new IdentifierNode(el.name()),
                el.t().typeId()))
          .toList(),
          function.body().accept(this, payload));
    }

    @Override
    public Tree visitRoot(Root root, Object payload) {
      AST.VmyAST _root = new AST.VmyAST();
      Tree body = root.body();
      _root.root = Objects.isNull(body) ? null : root.body().accept(this, payload);
      return _root;
    }

    @Override
    public <T extends Expression> Tree visitListExpr(ListExpr<T> expr, Object payload) {
      return null;
    }

    @Override
    public Tree visitReturnExpr(ReturnExpr expr, Object payload) {
      return new Return(expr.body().accept(this, payload));
    }

    @Override
    public Tree visitTypeExpr(TypeExpr expr, Object payload) {
      return null;
    }

    @Override
    public Tree visitCallExpr(CallExpr expr, Object payload) {
      return new CallNode(
          expr.callId(),
          new ListExpression(listMap(expr.params().body(), payload)));
    }

    @Override
    public Tree visitIdExpr(IdExpr expr, Object payload) {
      return new IdentifierNode(expr.name());
    }

    List<Tree> listMap(List<? extends Tree> origin,Object payload){
      return origin
          .stream()
          .map(el -> el.accept(this, payload))
          .toList();
    }
  }

}

package com.silence.vmy.compiler.transform;

import com.silence.vmy.compiler.oldIR.IdentifierNode;
import com.silence.vmy.compiler.oldIR.NumberLiteral;
import com.silence.vmy.compiler.tree.*;

import java.util.function.ToIntFunction;

public abstract class IrTransforms {
  static class toOldIdentifier implements Transformer<IdExpr, IdentifierNode> {
    @Override
    public IdentifierNode apply(IdExpr idExpr) {
      return new IdentifierNode(idExpr.name());
    }
  }

  static class Unary2NumberLiteral implements Transformer<Unary, NumberLiteral> {
    @Override
    public NumberLiteral apply(Unary unary) {
      Tree body = unary.body();
      int flag = 1; //
      ToIntFunction<Unary> getSign = (Unary tree) -> switch (tree.tag()) {
          case Sub -> -1; // -
          default -> 1; // + and others
      };
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
  }


}

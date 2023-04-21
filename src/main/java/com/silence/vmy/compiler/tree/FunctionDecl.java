package com.silence.vmy.compiler.tree;

import java.util.List;
import java.util.Objects;

public record FunctionDecl(
    String name,
    List<VariableDecl> params,
    TypeExpr ret,
    BlockStatement body,
    long position
    ) implements Statement{

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitFunctionDecl(this, payload);
  }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    if(visitor.enterFunctionDecl(this, t)){
      return visitor.leaveFunctionDecl(setBody((BlockStatement)body.accept(visitor, t)), t);
    }
    return this;
  }

  private FunctionDecl setBody(BlockStatement states){
    if(states == body){
      return this;
    }
    return new FunctionDecl(name, params, ret, states, position);
  }

  @Override public Tag tag() { return Tag.Fun; }
  @Override public String toString() { 
    StringBuilder sb = new StringBuilder("(");
    for(int i=0; i<params.size() - 1; i++){
      VariableDecl decl = params.get(0);
      sb.append(decl + ",");
    }
    if(params.size() > 0) {
      sb.append(params.get(params.size() - 1));
    }
    sb.append(") => " + (Objects.isNull(ret) ? "?" : ret.typeId()));
    return "Fn: " + sb.toString() + "{\n" +
      body.toString() + "\n" +
      "}";
  }
}

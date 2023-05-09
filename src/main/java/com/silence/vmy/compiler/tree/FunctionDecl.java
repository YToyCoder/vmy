package com.silence.vmy.compiler.tree;

import java.util.List;
import java.util.Objects;

public interface FunctionDecl extends Statement {

  public abstract String name();
  public abstract BlockStatement body();
  public abstract TypeExpr ret();
  public abstract List<VariableDecl> params();
  public static 
  FunctionDecl create(
    String name, 
    List<VariableDecl> params, 
    TypeExpr r, 
    BlockStatement body, 
    long position) {
    return new FnDeclImpl(name, params, r, body, position);
  }

  static record FnDeclImpl (
    String name,
    List<VariableDecl> params,
    TypeExpr ret,
    BlockStatement body,
    long position
    ) implements FunctionDecl {

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
      return new FnDeclImpl(name, params, ret, states, position);
    }

    @Override public Tag tag() { return Tag.Fun; }
    @Override public  String toString() {
      return fnDeclToString(this);
    }
  }
  default String fnDeclToString(FunctionDecl fn)
  {
    if(fn == null) return "null"; 
    StringBuilder sb = new StringBuilder("(");
    for(int i=0; i<params().size() - 1; i++){
      VariableDecl decl = params().get(0);
      sb.append(decl + ",");
    }
    if(params().size() > 0) {
      sb.append(params().get(params().size() - 1));
    }
    sb.append(") => " + (Objects.isNull(ret()) ? "?" : ret().typeId()));
    return "Fn(" + name() + "): " + sb.toString() + "{\n" +
      body().toString() + "\n" +
      "}";
  }
}

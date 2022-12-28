package com.silence.vmy.compiler.tree;

public abstract class LiteralExpression extends BaseTree implements Expression{

  public LiteralExpression(Kind kind) {
    this.kind = kind;
  }

  @Override
  public <R,T> R accept(TreeVisitor<R,T> visitor, T payload) {
    return visitor.visitLiteral(this, payload);
  }

  @Override
  public BaseTree.Tag tag() {
    return null;
  }

  public enum Kind {
    Int,
    String,
    Boolean,
    Double
  }

  private final Kind kind;

  public boolean isInt(){
    return kind == Kind.Int;
  }
  public boolean isDouble() {
    return kind == Kind.Double;
  }
  public boolean isBoolean(){
    return kind == Kind.Boolean;
  }
  public boolean isString(){
    return kind == Kind.String;
  }

  public abstract Object literal(); // string or function

  public static LiteralExpression ofStringify(String content, Kind kind){
    return new Stringify(kind, content);
  }

  private static class Stringify extends LiteralExpression {
    private final String content;
    public Stringify(Kind kind, String content) {
      super(kind);
      this.content = content;
    }

    @Override
    public Object literal() {
      return content;
    }
  }

}

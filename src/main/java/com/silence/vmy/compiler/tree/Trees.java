package com.silence.vmy.compiler.tree;

public abstract class Trees {
  // CompileUnit -> a file
  protected static class CompileUnit extends BaseTree implements Root{
    private final BaseTree body;

    protected CompileUnit(BaseTree tree) {
      this.body = tree;
    }

    @Override
    public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
      return visitor.visitRoot(this,payload);
    }

    @Override
    public Tree body() {
      return body;
    }
  }

  public static CompileUnit createCompileUnit(BaseTree content){
    return new CompileUnit(content);
  }

}

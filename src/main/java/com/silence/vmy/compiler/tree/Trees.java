package com.silence.vmy.compiler.tree;

public abstract class Trees {
  // CompileUnit -> a file
  protected static class CompileUnit extends BaseTree implements Root{
    private final Tree body;

    protected CompileUnit(Tree tree) {
      this.body = tree;
    }

    @Override
    public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
      return visitor.visitRoot(this,payload);
    }

    @Override
    public <T> Tree accept(TVisitor<T> visitor, T t) {
      if(visitor.enterRoot(this, t))
        return visitor.leaveRoot(this, t);
      return this;
    }

    @Override
    public Tree body() {
      return body;
    }

    @Override
    public Tag tag() {
      return Tag.Root;
    }
  }

  public static CompileUnit createCompileUnit(Tree content){
    return new CompileUnit(content);
  }

}

package com.silence.vmy.compiler.tree;

import java.util.Objects;

public abstract class Trees {
  // CompileUnit -> a file
  public static class CompileUnit extends BaseTree implements Root{
    private final Tree body;
    private String file;
    @Override
    public String file_name() { return file; }

    protected CompileUnit(Tree tree, String _f) {
      this.body = tree;
      file = _f;
    }

    @Override
    public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
      return visitor.visitRoot(this,payload);
    }

    @Override
    public <T> Tree accept(TVisitor<T> visitor, T t) {
      if(visitor.enterRoot(this, t))
        return visitor.leaveRoot(setBody(body.accept(visitor, t)), t);
      return this;
    }
    private CompileUnit setBody(Tree tree){
      if(tree == body) {
        return this;
      }
      return createCompileUnit(tree); 
    }

    @Override public Tree body() { return body; }
    @Override public Tag tag() { return Tag.Root; }
    @Override public String toString() { return Objects.isNull(body) ?"null" : ">>root<< \n" + body.toString(); }
  }
  public static CompileUnit createCompileUnit(Tree content){ return new CompileUnit(content, ""); }
  public static CompileUnit createCompileUnit(Tree content, String file){ return new CompileUnit(content, file); }

}

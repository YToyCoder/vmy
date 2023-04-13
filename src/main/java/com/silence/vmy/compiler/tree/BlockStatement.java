package com.silence.vmy.compiler.tree;

import java.util.ArrayList;
import java.util.List;

public record BlockStatement(List<Tree> exprs, long position) implements Statement{
  @Override
  public <R,T> R accept(TreeVisitor<R,T> visitor, T payload) {
    return visitor.visitBlock(this, payload);
  }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    return visitor.enterBlock(this, t) ? visitor.leaveBlock(doWithList(visitor, t), t) : this;
  }

  <T> BlockStatement doWithList(TVisitor<T> visitor, T t){
    boolean anyChanged = false;
    List<Tree> ret = new ArrayList<>(exprs.size());
    for(Tree el : exprs){
      Tree mayChanged = el.accept(visitor, t);
      if(mayChanged != el){
        anyChanged = true;
      }
      ret.add(mayChanged);
    }
    return anyChanged ? new BlockStatement(ret, position) : this;
  }

  @Override
  public Tag tag() {
    return null;
  }
}

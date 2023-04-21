package com.silence.vmy.compiler.tree;

import java.util.List;
import java.util.ArrayList;

public record IfStatement(
    ConditionStatement ifStatement,
    List<ConditionStatement> elif,
    BlockStatement el
    ) implements Statement {

  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) { return visitor.visitIfStatement(this, payload); }
  @Override
  public Tag tag() { return Tag.If; }
  @Override
  public long position() { return ifStatement.position(); }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    if(visitor.enterIfStatement(this, t)) {
      return visitor.leaveIfStatement(
          handleIf(ifStatement, visitor, t)
          .handleElif(visitor, t)
          .handleElse(visitor, t),
          t);
    }
    return this;
  }

  private <T> IfStatement handleIf(ConditionStatement state, TVisitor<T> visitor, T t)
  {
    var mayChanged = handleCondition(state, visitor, t);
    if(mayChanged == ifStatement){
      return this;
    }
    return new IfStatement(mayChanged, elif, el);
  }

  private <T> ConditionStatement handleCondition(
      ConditionStatement state,
      TVisitor<T> visitor, 
      T t) 
  {
    Tree condition = state.condition().accept(visitor, t);
    BlockStatement block = (BlockStatement) state.block().accept(visitor, t);
    if(condition == state.condition() && block == state.block()) {
      return state;
    }
    return new ConditionStatement(condition, block, state.tag(), state.position());
  }

  private <T> IfStatement handleElif(TVisitor<T> visitor, T t){
    boolean anyChanged = false;
    List<ConditionStatement> states = new ArrayList<>(elif.size());
    for(ConditionStatement state : elif){
      var mayChanged = handleCondition(state, visitor, t);
      if(mayChanged != state){
        anyChanged = true;
      }
      states.add((ConditionStatement) mayChanged);
    }
    return anyChanged ? new IfStatement(ifStatement, states, el) : this;
  }

  private <T> IfStatement handleElse(TVisitor<T> visitor, T t){
    if(el == null) return this;
    BlockStatement block = (BlockStatement) el.accept(visitor, t);
    if(block == el){
      return this;
    }
    return new IfStatement(ifStatement, elif, block);
  }

  @Override public String toString() {
    StringBuilder sb = new StringBuilder();
    for(var el : elif){
      sb.append(el + "\n");
    }
    if(el != null) {
      sb.append(el);
    }else {
      return "" + ifStatement + "\n" + (sb.length() == 0 ? "" : sb.substring(0, sb.length() - 1));
    }
    return "" + ifStatement + "\n" + sb;
  }

}

package com.silence.vmy.compiler.tree;

import java.util.Map;
import java.util.HashMap;

public record VmyObject(Map<String, Expression> properties, Tag tag, long position) implements Expression {
  public VmyObject(Map<String, Expression> props, long position) {
    this(props, Tag.Obj, position);
  }

  @Override
  public <R,T> R accept(TreeVisitor<R,T> visitor, T payload) { return visitor.visitVmyObject(this, payload); }

  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    if(visitor.enterVmyObject(this, t))
      return  visitor.leaveVmyObject(handleProperties(visitor, t), t); 
    return this;
  }

  private <T> VmyObject handleProperties(TVisitor<T> visitor, T t) {
    boolean changed = false;
    Map<String, Expression> props = new HashMap<>();
    for(var entry : properties.entrySet()){
      var value = entry.getValue();
      var mayChanged = value.accept(visitor, t);
      if(mayChanged != value){
        changed = true;
      }
      props.put(entry.getKey(), (Expression)mayChanged);
    }
    return changed ? new VmyObject(props, tag, position) : this;
  }
}

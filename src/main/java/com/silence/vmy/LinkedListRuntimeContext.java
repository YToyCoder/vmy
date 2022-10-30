package com.silence.vmy;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

import com.silence.vmy.Runtime.Variable;

public class LinkedListRuntimeContext implements RuntimeContext{
  private LinkedList<Frame> frame_stack = new LinkedList<>();

  @Override
  public Frame current_frame() {
    return frame_stack.isEmpty() ? null : frame_stack.peekLast();
  }

  @Override
  public Variable get_variable(String name) {
    Iterator<Frame> riter = frame_stack.descendingIterator();
    Variable variable = null;
    while(riter.hasNext()){
      Frame frame = riter.next();
      variable = frame.local(name);
      if(Objects.nonNull(variable)) 
        break;
    }
    return variable;
  }

  @Override
  public Frame new_frame() {
    CommonFrame frame = CommonFrame.create();
    frame_stack.add(frame);
    return frame;
  }

  @Override
  public void put(String name, Variable head, Object value) {
    current_frame().put(name, head, value);
  }

  @Override
  public Frame exitCurrentFrame() {
    return frame_stack.isEmpty() ? null : frame_stack.pop();
  }

}

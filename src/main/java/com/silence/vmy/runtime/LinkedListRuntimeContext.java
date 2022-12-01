package com.silence.vmy.runtime;

import java.util.LinkedList;
import java.util.Objects;

import com.silence.vmy.runtime.Runtime.Variable;

public class LinkedListRuntimeContext implements RuntimeContext {
  private LinkedList<Frame> frame_stack = new LinkedList<>();

  @Override
  public Frame current_frame() {
    return frame_stack.isEmpty() ? null : frame_stack.peekLast();
  }

  @Override
  public Variable get_variable(String name) {
    Frame frame = current_frame();
    return Objects.isNull(frame) ? null : frame.local(name);
  }

  @Override
  public Frame new_frame() {
    CommonFrame frame = CommonFrame.create(frame_stack.isEmpty() ? null : frame_stack.peek());
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

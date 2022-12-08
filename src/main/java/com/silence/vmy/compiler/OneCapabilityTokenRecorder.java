package com.silence.vmy.compiler;

import com.silence.vmy.compiler.deprecated.Token;

import java.util.Objects;

public class OneCapabilityTokenRecorder implements TokenHistoryRecorder {
  private Token the_one;

  @Override
  public TokenHistoryRecorder record_to_history(Token token) {
    the_one = Objects.requireNonNull(token);
    return this;
  }

  @Override
  public Token last() {
    if(!has_history())
      throw new RecordHistoryEmptyException("");
    return the_one;
  }

  @Override
  public Token get(int index) {
    if(index > 0)
      throw new IndexOutOfBoundsException("index of out bounds for " + index);
    return the_one;
  }

  @Override
  public boolean has_history(int index) {
    return false;
  }

  @Override
  public boolean has_history() {
    return Objects.nonNull(the_one);
  }
  
}

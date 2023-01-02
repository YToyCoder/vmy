package com.silence.vmy.compiler;

public record Modifiers(int modifier) {
  public boolean is(final int _modifier){
    return (_modifier & this.modifier) > 0;
  }

  public static class Builder {
    private int state;

    public Builder(){
      this.state = 0;
    }
    Builder Const(){
      state = set(this.state, CVariableConst);
      return this;
    }

    Modifiers build(){
      return new Modifiers(this.state);
    }
  }

  public static Modifiers Empty = new Modifiers(0);

  private static int set(int origin, int modifier){
    return origin | modifier;
  }

  public static final int CVariableConst = 1;
}

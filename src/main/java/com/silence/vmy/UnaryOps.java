package com.silence.vmy;

// 一元操作
public enum UnaryOps {
  Print{
    @Override
    public Object apply(Object obj) {
      System.out.println(obj);
      return obj;
    }
  }
  ;

  public abstract Object apply(Object obj);
}

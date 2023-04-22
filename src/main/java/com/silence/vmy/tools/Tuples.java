package com.silence.vmy.tools;

public abstract class Tuples {
  public static class Tuple2<T,U> {
    public final T _1;
    public final U _2;
    public Tuple2(T a, U b){
      _1 = a;
      _2 = b;
    }
  }

  public static <T,U> Tuple2<T,U> tuple(T a, U b){ return new Tuple2<>(a, b); }
}

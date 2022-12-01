package com.silence.vmy.tools;

public interface Ordering<T> {
  int compare(T other);
}

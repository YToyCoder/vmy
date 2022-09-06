package com.silence.vmy;

public interface Ordering<T> {
  int compare(T other);
}

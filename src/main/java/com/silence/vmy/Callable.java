package com.silence.vmy;

/**
 * builtin function support, every builtin function should implement this interface
 */
public interface Callable {
  Object call(Object ...params);
}

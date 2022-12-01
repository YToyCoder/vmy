package com.silence.vmy.runtime;

/**
 * builtin function support, every builtin function should implement this interface
 */
public interface Callable {
  Object call(Object ...params);
}

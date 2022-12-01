package com.silence.vmy.runtime;

// a pool to store obj in vmy
public interface ObjPool {
  /**
   * put obj to obj pool
   * @param identity
   * @param obj
   */
  void put(Long identity, Object obj);

  /**
   * get obj from obj pool
   * @param identity
   * @return
   */
  Object get(Long identity);

  boolean exists(Long identity);
}

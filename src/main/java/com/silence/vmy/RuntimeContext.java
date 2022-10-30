package com.silence.vmy;

import com.silence.vmy.Runtime.Variable;

/**
 * 运行时上下文{@link RuntimeContext}主要是用于保存运行时的变量和对象数据,以及创建、获取和退出运行时帧。
 */
public interface RuntimeContext {
  /**
   * 获取当前运行函数的运行时帧
   * @return {@link Frame}
   */
  Frame current_frame();

  /**
   * 获取某个变量
   * @param name 变量名称
   * @return {@link Variable}
   */
  Variable get_variable(String name);

  /**
   * 创建一个运行时帧，设置为当前运行时帧，并返回
   * @return
   */
  Frame new_frame();

  /**
   * 保存变量和值
   * @param name 变量名称
   * @param head 变量
   * @param value 变量引用值
   */
  void put(String name, Runtime.Variable head, Object value);

  /**
   * 退出当前运行时帧并返回。
   * @return {@link Frame}
   */
  Frame exitCurrentFrame();
}

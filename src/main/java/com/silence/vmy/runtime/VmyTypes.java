package com.silence.vmy.runtime;

import com.silence.vmy.runtime.VmyType;

public class VmyTypes {
  private VmyTypes(){}
  public enum BuiltinType implements VmyType {
    Nil,
    Int,
    Double,
    Boolean,
    Char,
    String,
    Table,
    Function,
    Any;
  }
}

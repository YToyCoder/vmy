package com.silence.vmy;

public class VmyTypes {
  private VmyTypes(){}
  public enum BuiltinType implements VmyType{
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

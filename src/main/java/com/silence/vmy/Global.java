package com.silence.vmy;

import java.util.*;

public class Global implements Frame {
  private Global(){}

  private static Global INSTANCE = new Global();

  public static Global getInstance() {
    return INSTANCE;
  }

  private Map<String, Object> primitives = new TreeMap<>();
  private final ObjPool objPool = Runtime.create_pool();
  private Map<String, Runtime.Variable> variables = new HashMap<>();

  @Deprecated
  public void put(String _name, Object _value){
    if( 
      Objects.isNull(_value) || 
      _value instanceof Number || 
      _value instanceof Boolean || 
      _value instanceof Character
    ){

      primitives.put(_name, _value);

    }
  }

  @Deprecated
  public boolean exists(String _name){
    return primitives.containsKey(_name);
  }

  @Deprecated
  public Object get(String _name){
    return primitives.get(_name);
  }

  @Override
  public Runtime.Variable local(String _name) {
    return variables.get(_name);
  }

  @Override
  public void put(String name, Runtime.Variable head, Object value) {

    long hash_code;

    if(Objects.nonNull(value))
      if(
        Utils.isType(head, VmyTypes.BuiltinType.Table) && 
        !objPool.exists(hash_code = value.hashCode())
      ){
        objPool.put(hash_code, value);
        head.setValue(hash_code);
      }else head.setValue(value);

    variables.putIfAbsent(name, head);

  }

  @Override
  public Object get_obj(Long identity) {
    return objPool.get(identity);
  }
}

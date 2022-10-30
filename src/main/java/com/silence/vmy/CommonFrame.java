package com.silence.vmy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CommonFrame implements Frame{

  private ObjPool objPool = Runtime.create_pool();
  private Map<String, Runtime.Variable> variables = new HashMap<>();

  private CommonFrame(){
  }

  static CommonFrame create(){
    return new CommonFrame();
  }

  @Override
  public Runtime.Variable local(String _name) {
    return variables.get(_name);
  }

  @Override
  public void put(String name, Runtime.Variable head, Object value) {
    if(Objects.nonNull(value)){
      /**
       * 保存变量和值，
       * 如果变脸是基础类型Int\Double\Boolean\Character,那么将数据放入head的value，
       * 如果不是则则将value的hashcode放入head的value
       * 并且将hashcode作为key，value作为值存放在map里面
       */
      if(
          value instanceof Number ||
          value instanceof Boolean ||
          value instanceof Character
      ) head.setValue(value);
      else {
        long hash_code = value.hashCode();
        if(!objPool.exists(hash_code))
          objPool.put(hash_code, value);
        head.setValue(hash_code);
      }
      variables.putIfAbsent(name, head);
    }
  }

  @Override
  public Object get_obj(Long identity) {
    return objPool.get(identity);
  }
}

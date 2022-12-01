package com.silence.vmy.runtime;

import com.silence.vmy.runtime.FunctionSupport.FunctionFactory;
import com.silence.vmy.runtime.FunctionSupport.FunctionRegister;
import com.silence.vmy.runtime.FunctionSupport.FunctionType;
import com.silence.vmy.compiler.Identifiers;
import com.silence.vmy.tools.Utils;

import java.util.*;


public class BuiltinOps 
  implements FunctionRegister, FunctionFactory
{
  private BuiltinOps(){}

  private static BuiltinOps INSTANCE;

  private static void init(){

    INSTANCE = new BuiltinOps();
    INSTANCE.register_builtins();

  }

  static {
    init();
  }

  public static BuiltinOps builtinOps(){
    return INSTANCE;
  }

  private TreeMap<FunctionType, List<Callable>> type_mapper = new TreeMap<>(Utils::function_type_compare);
  private TreeMap<String, List<Callable>> name_mapper = new TreeMap<>();

  @Override
  public Callable get_function(String name, FunctionType type) {

    // just looking by name
    List<Callable> with_name = name_mapper.get(name);
    return Objects.isNull(with_name) ? null : with_name.get(0);

  }

  @Override
  public void register(String name, FunctionType type, Callable callable) {

    if(name_mapper.containsKey(name))
      Utils.warning("registered function : " + Utils.function_to_string(name, type));
    type_mapper.computeIfAbsent(type, key -> new ArrayList<>()).add(callable);
    name_mapper.computeIfAbsent(name, key -> new ArrayList<>()).add(callable);

  }

  /**
   * register builtin functions (call)
   */
  private void register_builtins(){
    register(
        Identifiers.Print,
        FunctionSupport.functionType(FunctionSupport.Builtin, VmyTypes.BuiltinType.Any),
        params -> {
          for(Object param : params)
            System.out.print(param);
          System.out.println();
          return null;
        }
    );
  }

}

package com.silence.vmy.runtime;

import com.silence.vmy.compiler.deprecated.FunctionNode;
import com.silence.vmy.tools.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * all builtin call can get here
 */
public class FunctionSupport {
  private FunctionSupport(){}

  // call a function
  public static Object call(String name, List<Object> params){
    Callable func = lookup_function(name, params);
    return func.call(params.toArray(new Object[0]));
  }

  public static boolean is_builtin_func(String name){
    return Objects.nonNull(
      BuiltinOps
      .builtinOps()
      .get_function(name, null)
    );
  }

  /**
   * lookup the function
   * @param name function name
   * @param params function params
   * @return {@link Callable}
   */
  private static Callable lookup_function(String name, List<Object> params){
    FunctionType function_type = functionType(
            Unknown ,
            /* params' types */params.stream()
              .map(Utils::get_obj_type)
              .collect(Collectors.toList())
              .toArray(new VmyType[0])
    );
    Callable func = BuiltinOps.builtinOps().get_function(
        name,
        function_type
    );
    if(Objects.isNull(func))
      throw new RuntimeException("function " + Utils.function_to_string(name, function_type) + " not founded");
    return func;
  }

  public static final int Builtin = 0;
  public static final int UserDefined = 1;
  public static final int Native = 2;
  public static final int Unknown = 3;

  public interface FunctionType extends VmyType {
    /**
     * @return types of all params
     */
    List<VmyType> types();

    /**
     * a function's params can see as an array, if you want to get the param of 0 , it can be seen as params[0]
     * @param i
     * @return the type of param in location i, if 'i' is out of length for params return null
     */
    VmyType param_type(int i);

    /**
     * @return Function tag , mark for user defined(user defined using vmy script) , builtin(builtin call) or Native(Write By Java)
     *
     * @see FunctionSupport#Builtin
     * @see FunctionSupport#Native
     * @see FunctionSupport#UserDefined
     */
    int tag();
  }

  /**
   * register a function
   */
  public interface FunctionRegister {
    void register(String name, FunctionType type, Callable callable);
  }

  /**
   * create a function
   */
  public interface FunctionFactory {
    /**
     * get a {@link Callable} by function name and function type
     * @param name function name
     * @param type function type
     * @return {@link Callable} or {@code null}
     */
    Callable get_function(String name, FunctionType type);
  }

  public static FunctionType functionType(int _tag, VmyType ...types){
    return new DefaultFuncTypeImpl(new ArrayList<>(List.of(types)), _tag);
  }

  private record DefaultFuncTypeImpl(
    List<VmyType> types, 
    int tag
  ) implements FunctionType {

    @Override
    public VmyType param_type(int i) {
      return i < types.size() ? types.get(i) : null;
    }
  }

  public record ASTFunction(FunctionNode func) {
  }

  static ASTFunction create_ast_function(FunctionNode func){
    return new ASTFunction(func);
  }
}

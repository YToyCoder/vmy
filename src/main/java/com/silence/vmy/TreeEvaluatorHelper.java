package com.silence.vmy;

import java.util.Objects;

public class TreeEvaluatorHelper {

  public static Object get_value(Object obj, Frame frame){
    if(obj instanceof Runtime.VariableWithName variable) {
      return Runtime.get_value(variable.name(), frame);
    }
    return obj;
  }

  public static void can_assign(VmyType variable_type, VmyType expression_type){
    if(!Utils.equal(variable_type, expression_type))
      throw new ASTProcessingException("type " + expression_type + " can not be assigned to type " + variable_type);
  }

  public static Runtime.VariableWithName get_variable(String name, Frame frame){
    Runtime.Variable variable = frame.local(name);
    if(Objects.isNull(variable))
      throw new EvaluatException("variable " + name + " haven't declared!");
    return Utils.variable_with_name(name, variable);
  }

  public static boolean check_if_value_can_be_assign_to(String variable_name, Object value, Frame frame){
    Runtime.VariableWithName identifier_variable = TreeEvaluatorHelper.get_variable(variable_name, frame);
    try {
      can_assign(identifier_variable.getType(), Utils.get_obj_type(value));
    }catch (ASTProcessingException ast_e){
      throw new ASTProcessingException(String.format(
          "%s : variable name - %s , value string - %s",
          ast_e.getMessage(),
          variable_name, value.toString()));
    }
    return true;
  }

  public static void assign_to(
      String variable_name,
      Runtime.Variable variable,
      Object value,
      Frame frame){
    frame.put(variable_name, variable, value);
  }
}

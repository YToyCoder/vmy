package com.silence.vmy.runtime;

public class Evaluators {
    private static VmyTreeEvaluator Evaluator = new VmyTreeEvaluator();
    private static VariableStoreTreeEvaluator VSTEvaluator = new VariableStoreTreeEvaluator();

    public static Evaluator defaultTreeEvaluator() {
      return Evaluator;
    }

    public static Evaluator variableStoreTreeEvaluator(){
      return VSTEvaluator;
    }

    public static Evaluator evaluator(boolean create){
      return create ? new VariableStoreTreeEvaluator() : variableStoreTreeEvaluator();
    }
}

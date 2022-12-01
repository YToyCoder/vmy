package com.silence.vmy;

import com.silence.vmy.compiler.AST;
import com.silence.vmy.runtime.Evaluators;
import com.silence.vmy.tools.Eval;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class EvalTest {


  @Test
  public void evalTest(){
    assertEquals( 1 + 2 * (3 + 4), Eval.eval("1 + 2 * ( 3 + 4 )", Evaluators.variableStoreTreeEvaluator()));
    assertEquals( 1 + 2 * (3 + 4.0), Eval.eval("1 + 2 * ( 3 + 4.0 )", Evaluators.variableStoreTreeEvaluator()));
    assertEquals( 1 + 2 * (3 + 4) * (5) - 1, Eval.eval("1 + 2 * ( 3 + 4 ) * ( 5 )  - 1", Evaluators.variableStoreTreeEvaluator()));
  }
  @Test
  public void evalTest1(){
    assertEquals(1 + 2 * 3 * 4, Eval.eval("1 + 2 * 3 * 4", Evaluators.variableStoreTreeEvaluator()));
    assertEquals(1 + 2 / 3 * 4 * 7, Eval.eval("1 + 2 / 3 * 4 * 7", Evaluators.variableStoreTreeEvaluator()));
    assertEquals(1 + 14 / (3 + 4), Eval.eval("1 + 14 / (3 + 4)", Evaluators.variableStoreTreeEvaluator()));
  }

  @Test
  public void evalTest2(){
    assertEquals(1 + 2 * 3 * 4, Eval.eval("let a : Int = 1 + 2 * 3 * 4", Evaluators.variableStoreTreeEvaluator()));
    assertEquals(1 + 2 * 3 * 4, Eval.eval("let k = 1 + 2 * 3 * 4", Evaluators.variableStoreTreeEvaluator()));
    assertEquals(1 + 2 / 3 * 4 * 7, Eval.eval("let b : Int = 1 + 2 / 3 * 4 * 7", Evaluators.variableStoreTreeEvaluator()));
    assertEquals(1 + 14 / (3 + 4), Eval.eval("let c : Int = 1 + 14 / (3 + 4)", Evaluators.variableStoreTreeEvaluator()));
  }

  @Test
  public void concat_two_string(){
    List<String> ls = List.of(
        "\"111\"",
        "\"222\"",
        "\"333\"",
        "\"444\""
    );
    for(int i=0; i < ls.size() - 1; i++){
      for(int j=i + 1; j < ls.size(); j++ ){
        cts(ls.get(i), ls.get(j));
      }
    }
  }

  @Test
  public void eval_script_test(){
    FileInputScannerTestUtils.do_with_instance(
        FileInputScannerTestUtils.ofScript("number_literal_token_support_test.vmy"),
        scanner -> {
          Evaluators.evaluator(true).eval(AST.build(scanner));
        }
    );
  }

  @Test
  public void eval_script_while_loop(){
    FileInputScannerTestUtils.do_with_instance(
        FileInputScannerTestUtils.ofScript("while_loop_test.vmy"),
        FileInputScannerTestUtils.eval_with_scanner()
    );
  }

  @Test
  public void eval_negative_number(){
    FileInputScannerTestUtils.do_with_instance(
        FileInputScannerTestUtils.ofScript("negative_number_test.vmy"),
        FileInputScannerTestUtils.eval_with_scanner()
    );
  }

  @Test
  public void eval_if_else(){
    FileInputScannerTestUtils.do_with_instance(
        FileInputScannerTestUtils.ofScript("if_else_eval_test.vmy"),
        FileInputScannerTestUtils.eval_with_scanner()
    );
  }

  void cts(String v1, String v2){
    assertEquals(
        v1.substring(1, v1.length() - 1) + v2.substring(1, v2.length() - 1),
        Eval.eval(v1 + " ++ " + v2, Evaluators.variableStoreTreeEvaluator())
    );
  }


  @Test
  public void eval_function(){
    FileInputScannerTestUtils.do_with_instance(
        FileInputScannerTestUtils.ofScript("function_support.vmy"),
        FileInputScannerTestUtils.eval_with_scanner()
    );
  }
}

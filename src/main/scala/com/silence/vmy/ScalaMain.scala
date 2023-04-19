package com.silence.vmy

import com.silence.vmy.evaluate.TreeEmulator
import com.silence.vmy.tools.Eval

object ScalaMain {

  private def evalScript(script: String) : Unit = {
    println(s"parsing script ${script}")
    val ast = Eval.parsing(script, true)
    // println(ast)
    ast.accept(new TreeEmulator(), null)
  }

  def main(args: Array[String]): Unit = {
    for(file <- args)
      evalScript(file)
  }
}

package com.silence.vmy

import com.silence.vmy.evaluate.TreeEmulator
import com.silence.vmy.tools.Eval

object ScalaMain {

  private def evalScript(script: String) : Unit = {
    println(s"parsing script ${script}")
    val ast = Eval.parsing(script, true)
    // println(ast)
    println("parsing finished")
    println("starting eval ...")
    ast.accept(new TreeEmulator(), null)
    println("eval finished")
  }

  def main(args: Array[String]): Unit = {
    for(file <- args)
      evalScript(file)
  }
}

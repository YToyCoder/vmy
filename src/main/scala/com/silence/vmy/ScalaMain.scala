package com.silence.vmy

import com.silence.vmy.evaluate.TreeEmulator
import com.silence.vmy.tools.Eval
import com.silence.vmy.tools.Log
import com.silence.vmy.compiler.ConstFold

object ScalaMain extends Log {
  val debug = true

  private def evalScript(script: String) : Unit = {
    if( debug ) {
      println(s"parsing script ${script}")
    }
    val ast = Eval.parsing(script, true)
    val foldTree = ast.accept(new ConstFold(), 0)
    if(debug) {
      log(ast.toString)
      log("#" * 20)
      log(foldTree.toString)
      log("parsing finished")
      log("starting eval ...")
    }
    ast.accept(new TreeEmulator(), null)
    if(debug)
      log("eval finished")
  }

  def main(args: Array[String]): Unit = {
    for(file <- args)
      evalScript(file)
  }
}

package com.silence.vmy

import com.silence.vmy.evaluate.TreeEmulator
import com.silence.vmy.tools.Eval
import com.silence.vmy.tools.Log
import com.silence.vmy.compiler.ConstFold

object ScalaMain extends Log {

  private def evalScript(script: String, debug: Boolean) : Unit = {
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
    val emulator = new TreeEmulator()
    emulator.debug = debug
    foldTree.accept(emulator, null)
    if(debug)
      log("eval finished")
  }

  def main(args: Array[String]): Unit = {
    val (debug, files) = handleArgs(args)
    for(file <- files)
      evalScript(file, debug)
  }

  def handleArgs(args: Array[String]): (Boolean, Array[String]) = {
    var debug = false
    var res: List[String] = Nil
    for(i <- args){
      if(i == "-dbg") debug = true
      else res = i :: res
    }
    (debug, res.toArray)
  }
}

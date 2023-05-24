package com.silence.vmy

import com.silence.vmy.evaluate.TreeEmulator
import com.silence.vmy.evaluate.EmulatorContext
import com.silence.vmy.compiler.CompileContext
import com.silence.vmy.tools.Eval
import com.silence.vmy.tools.Log
import com.silence.vmy.compiler.ConstFold
import com.silence.vmy.compiler.tree._
import com.silence.vmy.compiler.Compiler
import com.silence.vmy.compiler.LCompiler
import com.silence.vmy.compiler.Compilers.CompileUnit
import com.silence.vmy.compiler.ConstFoldPhase
import com.silence.vmy.compiler.CompilerPhase
import com.silence.vmy.compiler.CompileFinishPhase
import com.silence.vmy.compiler.UpValuePhase
import com.silence.vmy.compiler.PerEvaluatingPhase
import com.silence.vmy.compiler.CompileUnit.wrapAsCompileUnit

import scala.annotation.tailrec
import java.io.File
import com.silence.vmy.shared.EmulatingValue.EVGlobal
import com.silence.vmy.shared.EmulatingValue.EVObj
import com.silence.vmy.tools.TreePrinter

object ScalaMain extends Log {
  private val TreePrinter = new TreePrinter()

  private def evalScript(script: String, debug: Boolean) : Unit = {
    if( debug ) {
      println(s"parsing script ${script}")
    }
    val file = File(script).getAbsoluteFile()
    val ast = wrapAsCompileUnit(Eval.parsing(file.getAbsolutePath(), true))
    val context = new CompileContext()
    // val foldTree = ast.accept(new ConstFold() {}, context)
    val compiledTree = LCompiler.compile(context, ast)
    if(debug) {
      log("origin tree => \n" + TreePrinter.tree_as_string(ast.node()))
      log("compiled tree => \n")
      log(TreePrinter.tree_as_string(compiledTree.node()))
      log("#" * 20)
      log("parsing finished")
      log("starting eval ...")
    }
    val emulator = new TreeEmulator(context, LCompiler)
    emulator.debug = debug
    emulator.run(compiledTree.node())
    if(debug)
      log("eval finished")
  }

  def main(args: Array[String]): Unit = {
    val (debug, files) = handleArgs(args)
    // println(s"EVGlobal is EVObj => ${EVGlobal.isInstanceOf[EVObj]}")
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

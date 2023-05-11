package com.silence.vmy

import com.silence.vmy.evaluate.TreeEmulator
import com.silence.vmy.evaluate.EmulatorContext
import com.silence.vmy.compiler.CompileContext
import com.silence.vmy.tools.Eval
import com.silence.vmy.tools.Log
import com.silence.vmy.compiler.ConstFold
import com.silence.vmy.compiler.tree._
import com.silence.vmy.compiler.Compiler
import com.silence.vmy.compiler.Compilers.CompileUnit
import com.silence.vmy.compiler.ConstFoldPhase
import com.silence.vmy.compiler.CompilerPhase
import com.silence.vmy.compiler.CompileFinishPhase
import com.silence.vmy.compiler.UpValuePhase
import com.silence.vmy.compiler.PerEvaluatingPhase
import com.silence.vmy.compiler.CompileUnit.wrapAsCompileUnit

import scala.annotation.tailrec

object LCompiler extends Compiler[CompileContext] 
{
  val phases: List[CompilerPhase] = 
    ConstFoldPhase :: 
      // UpValuePhase ::
      PerEvaluatingPhase ::
    CompileFinishPhase ::
    Nil
  def compile(context: CompileContext, unit: CompileUnit) =
  {
    @tailrec
    def doPhases(phases: List[CompilerPhase], unit: CompileUnit): CompileUnit= 
      phases match 
      {
        case Nil => unit
        case phase :: rest => 
          doPhases(rest, phase.run(context, unit))
      }
    doPhases(phases, unit)
  }
}

object ScalaMain extends Log {

  private def evalScript(script: String, debug: Boolean) : Unit = {
    if( debug ) {
      println(s"parsing script ${script}")
    }
    val ast = wrapAsCompileUnit(Eval.parsing(script, true))
    val context = new CompileContext()
    // val foldTree = ast.accept(new ConstFold() {}, context)
    val compiledTree = LCompiler.compile(context, ast)
    if(debug) {
      log("origin tree => \n" + ast.toString)
      log("#" * 20)
      log("compiled tree => \n" + compiledTree.toString)
      log("parsing finished")
      log("starting eval ...")
    }
    val emulator = new TreeEmulator(context, LCompiler)
    emulator.debug = debug
    compiledTree.node().accept(emulator, null)
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

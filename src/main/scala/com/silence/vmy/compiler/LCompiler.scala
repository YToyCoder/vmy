package com.silence.vmy.compiler

import scala.annotation.tailrec

import com.silence.vmy.compiler.Compilers.CompileUnit

object LCompiler extends Compiler[CompileContext] 
{
  val phases: List[CompilerPhase] = 
    ConstFoldPhase :: 
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
    if(unit.compiled()) unit
    else doPhases(phases, unit)
  }
}
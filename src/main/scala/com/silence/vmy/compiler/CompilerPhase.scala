package com.silence.vmy.compiler

import com.silence.vmy.compiler.tree._
import com.silence.vmy.evaluate.EmulatorContext
import Compilers.CompileUnit
import com.silence.vmy.compiler.CompileUnit.wrapAsCompileUnit


trait CompilerPhase extends Phase[CompileContext] 
{
  object ConstFoldPhase 
    extends ConstFold 
    with CompilerPhase 
  {
    // failed => null
    override def run(context: CompileContext, unit: CompileUnit) = 
    {
      if(unit == null || unit.node() == null) null
      else {
        unit.node().accept(this, context) match 
        {
          case unit: CompileUnit => unit
          case node => wrapAsCompileUnit(node)
          // handle by node
        }
      } 
    } 

  }
}


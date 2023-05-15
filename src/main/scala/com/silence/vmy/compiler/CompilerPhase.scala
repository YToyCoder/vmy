package com.silence.vmy.compiler

import com.silence.vmy.compiler.tree._
import com.silence.vmy.evaluate.EmulatorContext
import com.silence.vmy.compiler.CompileUnit.wrapAsCompileUnit
import Compilers.CompileUnit


trait CompilerPhase 
  extends Phase[CompileContext] 
  with PerCompileUnitTVisitor
{

  def doWithTopNode(node: Tree, context: CompileContext, visitor: PerCompileUnitTVisitor): Tree = 
  {
    node match {
      case CompiledFn(name, params, ret, body, ups, position) => {
        val validatedBody = body.accept(visitor, context).asInstanceOf[BlockStatement]
        if (validatedBody != body) 
          new CompiledFn(name, params, ret, validatedBody, ups, position)
        else node
      }
      case declFn : FunctionDecl => {
        val fn = declFn.asInstanceOf[FunctionDecl]
        val body = fn.body.accept(visitor, context).asInstanceOf[BlockStatement]
        if(body == fn.body) node
        else
          new CompiledFn(fn.name, fn.params, fn.ret, body, UpValues(null), fn.position)
      }
      case root: Root => {
        val rootNode = root.asInstanceOf[Root] 
        val body = rootNode.body.accept(visitor, context).asInstanceOf[BlockStatement]
        new CompiledFn("main", java.util.List.of(), null, body, UpValues(null), body.position)
      }
      case node => node.accept(visitor, context)
    }
  }

  final override def run(context: CompileContext, unit: CompileUnit) = {
    leaveVisit(context, phaseAction(context, enterVisit(context, unit)))
  }

  def phaseAction(context: CompileContext, unit: CompileUnit): CompileUnit 
}

object ConstFoldPhase 
  extends ConstFold 
  with CompilerPhase 
{
  // failed => null
  override def phaseAction(context: CompileContext, unit: CompileUnit) = 
  {
    if(unit == null || unit.node() == null) null
    else {
      doWithTopNode(unit.node, context, this) match {
        case unit: CompileUnit => unit
        case node => wrapAsCompileUnit(node)
      }
    } 
  } 
}

object PerEvaluatingPhase
  extends PerCompileUnitTVisitor
  with CompilerPhase
{
  override def phaseAction(context: CompileContext, unit: CompileUnit) = {
    unit.node match {
      case _fn @ CompiledFn(name, _, _, _, _, _) if name == "main" => {
        val fn = _fn.asInstanceOf[CompiledFn]
        if(!fn.compiled) {
          fn.compileFinish()
          val mainFnCall = CallExpr( -1, null, "main", new ListExpr(-1, null, java.util.List.of())) 
          new RootCompileUnit(new BlockStatement(java.util.List.of(fn, mainFnCall), -1))
        }
        else unit
      }
      case _ => unit
    }
  }

  class RootCompileUnit(_body: Tree) 
    extends Trees.CompileUnit (_body)
    with CompileUnit {
      def compiled() = true
      def node() = this
    }
}

object CompileFinishPhase
  extends PerCompileUnitTVisitor
  with CompilerPhase
{
  override def phaseAction(context: CompileContext, unit: CompileUnit) = {
    unit match {
      case fn: CompiledFn => 
        (fn.asInstanceOf[CompiledFn]).compileFinish()
        fn
      case _ => unit
    }
  }
}

object UpValuePhase 
  extends VariableDeclarationAndUpValuesChecking 
  with CompilerPhase
{

  override def phaseAction(context: CompileContext, unit: CompileUnit) = {
    if(unit == null || unit.node() == null) null
    else 
      doWithTopNode(unit.node(), context, this) match {
        case fn @ CompiledFn(name, params, ret, body, _, position) => 
          if fn.compiled() then fn
          else
            CompiledFn(name, params, ret, body, getUpvalues(), position)
        case node => wrapAsCompileUnit(node)
      }
  }

  override def enterVisit(context: CompileContext, unit: CompileUnit): CompileUnit = 
    cleanVariable()
    if(unit.isInstanceOf[CompiledFn]) {
      unit.asInstanceOf[CompiledFn].params.forEach(_.accept(this, context))
    }
    unit
}

package com.silence.vmy.compiler

import com.silence.vmy.compiler.tree._
import com.silence.vmy.evaluate.EmulatorContext
import Compilers.CompileUnit
import com.silence.vmy.compiler.CompileUnit.wrapAsCompileUnit


trait CompilerPhase extends Phase[CompileContext] 
{

  def doWithTopNode(node: Tree, context: CompileContext, visitor: PerCompileUnitTVisitor): Tree = 
  {
    node match 
    {
      case CompiledFn(name, params, ret, body, ups, position) => 
      {
        val validatedBody = body.accept(visitor, context).asInstanceOf[BlockStatement]
        if (validatedBody != body) 
          new CompiledFn(name, params, ret, validatedBody, ups, position)
        else node
      }
      case declFn : FunctionDecl => 
      {
        val fn = declFn.asInstanceOf[FunctionDecl]
        val body = fn.body.accept(visitor, context).asInstanceOf[BlockStatement]
        if(body == fn.body) node
        else
          new CompiledFn(fn.name, fn.params, fn.ret, body, null, fn.position)
      }
      case root: Root => 
      {
        val rootNode = root.asInstanceOf[Root] 
        val body = rootNode.body.accept(visitor, context).asInstanceOf[BlockStatement]
        if(body == rootNode.body) root
        else
          new CompiledFn("main", java.util.List.of(), null, body, null, body.position)
      }
    }
  }

}

object ConstFoldPhase 
  extends ConstFold 
  with CompilerPhase 
{
  // failed => null
  override def run(context: CompileContext, unit: CompileUnit) = 
  {
    if(unit == null || unit.node() == null) null
    else {
      doWithTopNode(unit.node, context, this) match 
      {
        case unit: CompileUnit => unit
        case node => wrapAsCompileUnit(node)
        // handle by node
      }
    } 
  } 
}

object PerEvaluatingPhase
  extends PerCompileUnitTVisitor
  with CompilerPhase
{
  override def run(context: CompileContext, unit: CompileUnit) =
  {
    val allDeclaration = unit.node
    val mainFnCall = CallExpr(
      -1, 
      null, 
      "main", 
      new ListExpr(-1, null, java.util.List.of())
    ) 
    new RootCompileUnit(new BlockStatement(java.util.List.of(allDeclaration, mainFnCall), -1))
  }

  class RootCompileUnit(body: Tree) 
    extends Trees.CompileUnit (body)
    with CompileUnit {
      def compiled() = true
      def node() = this
    }
}

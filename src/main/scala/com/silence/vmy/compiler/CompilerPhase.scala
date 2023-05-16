package com.silence.vmy.compiler

import com.silence.vmy.compiler.tree._
import com.silence.vmy.evaluate.EmulatorContext
import com.silence.vmy.compiler.CompileUnit.wrapAsCompileUnit
import Compilers.CompileUnit
import com.silence.vmy.compiler.RootCompileUnit

import java.{util as ju}
import java.util.stream.Collectors
import java.util.ArrayList


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
        root.body() match 
          // extract import and export
          case body : BlockStatement => {
            val (imports, exports, block) = extracte_import_and_export(body)
            val changedBody = block.accept(visitor, context).asInstanceOf[BlockStatement]
            val fn = new CompiledFn("main", java.util.List.of(), null, changedBody, UpValues(null), body.position)
            new RootCompileUnit(fn, -1, imports, exports)
          }
          case body => {
            val changedBody = body.accept(visitor, context).asInstanceOf[BlockStatement]
            val fn = new CompiledFn("name", ju.List.of(), null, changedBody, UpValues(null), body.position())
            new RootCompileUnit(fn, -1)
          }
      }
      case node => node.accept(visitor, context)
    }
  }

  final override def run(context: CompileContext, unit: CompileUnit) = {
    leaveVisit(context, phaseAction(context, enterVisit(context, unit)))
  }

  private def extracte_import_and_export(block: BlockStatement): (ju.List[ImportState],ju.List[ExportState], BlockStatement) = {
    def is_export_or_import(tree: Tree): Boolean = 
      tree.isInstanceOf[ImportState] || tree.isInstanceOf[ExportState]
        // val (is, not) = 
    val group = block.exprs().stream().collect(Collectors.groupingBy(is_export_or_import))
    val import_or_export = group.get(true).stream().collect(Collectors.groupingBy(_.isInstanceOf[ImportState]))
    ( import_or_export.get(true).asInstanceOf[ju.List[ImportState]], 
      import_or_export.get(false).asInstanceOf[ju.List[ExportState]], 
      new BlockStatement(group.get(false), block.position()))
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
    val (imports, exports) = try_to_get_import_export(unit)
    unit.node match {
      case _fn @ CompiledFn(name, _, _, _, _, _) if name == "main" => {
        val fn = _fn.asInstanceOf[CompiledFn]
        if(!fn.compiled) {
          fn.compileFinish()
          val mainFnCall = CallExpr( -1, null, "main", new ListExpr(-1, null, java.util.List.of())) 
          val block_elems = new ArrayList[Tree](imports.size() + exports.size() + 2)
          block_elems.addAll(imports)
          block_elems.addAll(exports)
          block_elems.addAll(ju.List.of(fn, mainFnCall))
          new RootCompileUnit(
            new BlockStatement(block_elems, -1),
            -1,
            imports,
            exports)
        }
        else unit
      }
      case _ => unit
    }
  }

  // only try RootCompileUnit
  private def try_to_get_import_export(unit: CompileUnit): (ju.List[ImportState], ju.List[ExportState]) = {
    unit match 
      case root: RootCompileUnit => (root.imports(), root.exports())
      case _ => (ju.List.of(), ju.List.of())
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

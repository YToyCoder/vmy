package com.silence.vmy.compiler

import com.silence.vmy.compiler.tree._
import com.silence.vmy.evaluate.EmulatorContext
import com.silence.vmy.compiler.CompileUnit.wrapAsCompileUnit
import Compilers.CompileUnit
import com.silence.vmy.compiler.RootCompileUnit

import java.{util as ju}
import java.util.stream.Collectors
import java.util.ArrayList
import scala.collection.mutable
import com.silence.vmy.evaluate.RegisterModuleTree


trait CompilerPhase 
  extends Phase[CompileContext] 
  with PerCompileUnitTVisitor
{

  // convert one export
  private def transform_export(ex: ExportState): ju.List[Tree] = {
      val one_ex = ex
      val exs = ju.ArrayList[Tree]()
      if(one_ex.isOne()){
        val exp = one_ex.getOne();
        ju.List.of(transform_export_exp_as_update_expression(exp))
      }
      else if(one_ex.isObjForm()){
        val exps = one_ex.getAll()
        exps.forEach{ e => 
          exs.add{ transform_export_exp_as_update_expression(e) }
        }
        exs
      }
      else ju.List.of()
  }

  // there will be two type of expression
  // 1. 
  // export id  => __export(id) = id
  // 2.
  // export id as new_name => __export(new_name) = id
  private def transform_export_exp_as_update_expression(ex_exp: ExportState.ExportExp): CallExpr = {
    val as = if(ex_exp.hasAlias()) ex_exp.alias() else ex_exp.name() 
    update_expression("__export", as, id_expression(ex_exp.name()))
  }

  private def id_expression(id: String): IdExpr = {
    new IdExpr(0, Tree.Tag.Id, id)
  }

  // it will be generate expression like => id(el, v_exp)
  private def update_expression(id: String, el: String, v_exp: Expression): CallExpr = {
      CallExpr.create(
        0, 
        id, 
        new ListExpr[Expression](
          0, 
          null,
          ju.List.of(string_literal_expression(el), v_exp)
        )
      ) 
  }

  private def string_literal_expression(literal: String): LiteralExpression = {
    LiteralExpression.ofStringify(literal, LiteralExpression.Kind.String)
  }

  def doWithTopNode(node: Tree, context: CompileContext, visitor: PerCompileUnitTVisitor): Tree = 
  {
    node match {
      case fn @ CompiledFn(name, params, ret, body, ups, position) => {
        val validatedBody = body.accept(visitor, context).asInstanceOf[BlockStatement]
        if (validatedBody != body) 
          new CompiledFn(name, params, ret, validatedBody, ups, position).setFile(fn.file_name())
          .setFile(fn.file_name())
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
            val changedBody = transform_as_export_pre_declaration(body.accept(visitor, context).asInstanceOf[BlockStatement])
            val fn = new CompiledFn("main", java.util.List.of(), null, transform_export_and_import(changedBody), UpValues(null), body.position)
            new RootCompileUnit(fn, -1)
            .setFile(root.file_name())
          }
          case body => {
            val changedBody = body.accept(visitor, context).asInstanceOf[BlockStatement]
            val fn = new CompiledFn("name", ju.List.of(), null, changedBody, UpValues(null), body.position())
            new RootCompileUnit(fn, -1)
            .setFile(root.file_name())
          }
      }
      case node => node.accept(visitor, context)
    }
  }

  final override def run(context: CompileContext, unit: CompileUnit) = {
    leaveVisit(context, phaseAction(context, enterVisit(context, unit)))
  }

  private def transform_export_and_import(block: BlockStatement): BlockStatement = {
    val _import_alias_map: ImportUriAliasMap = mutable.Map()
    def do_transform(tree: Tree): ju.List[Tree] = 
      tree match 
        case ex: ExportState => 
          transform_export(ex) 
        case ix: ImportState => transform_import(ix, _import_alias_map)
        case _ => ju.List.of(tree)
    val exp_list = ju.ArrayList[Tree]()
    block.exprs().forEach{ el =>
      exp_list.addAll(do_transform(el))
    }
    new BlockStatement(exp_list, block.position())
  }

  // >>>>>>>>>>>>>> import <<<<<<<<<<<<<<<<<
  // import naming alias rules 
  // all import will named like __vmy_ipt_$count__
  // $count will be increase by import number
  // if source file has two import from same uri
  // there will be one import-alias
  // the module-name will be transformed as variable declaration and assign to alias_named_variable
  type ImportUriAliasMap = mutable.Map[String, String]
  private def transform_import(ix: ImportState, ipt_map: ImportUriAliasMap): ju.List[Tree] = {
    val tree_ls = ju.ArrayList[Tree]()
    var ipt: ImportState = ix
    val uri = ix.uri()
    var alias_name =  "" // s"__vmy_ipt_${import_count}__"
    // 
    if(!(ipt_map contains uri)){
      val import_count = ipt_map.size
      alias_name = s"__vmy_ipt_${import_count}__"
      ipt_map.addOne((uri, alias_name))
      tree_ls.add(ImportState.create(uri, ImportState.createImportExp(alias_name, 0), 0))
    }else{
      alias_name = ipt_map(uri)
    }
    // variable declarations
    val get_ipt_name = (ipt_exp: ImportState.ImportExp) => 
      if(ipt_exp.hasAlias()) ipt_exp.alias() else ipt_exp.name()
    if(ix.isImportAsOne()){
      val ipt_1 = ix.oneImport()
      tree_ls.add(create_assign_to_decl_variable_exp(get_ipt_name(ipt_1), id_expression(alias_name)))
    }else if(ix.isElementImport()){
      val ipt_s = ix.elemImport()
      ipt_s.forEach{ ipt_1 => 
        // import {a as b} from uri <= a:name b:alias
        tree_ls.add(create_assign_to_decl_variable_exp(get_ipt_name(ipt_1), create_get_obj_member( alias_name, ipt_1.name())))
      }
    }
    tree_ls
  }

  private def create_get_obj_member(id: String, member: String): Expression = {
    new CallExpr(0,Tree.Tag.CallExpr, id, new ListExpr[Expression](0, null, ju.List.of(string_literal_expression(member))))
  }

  private def create_assign_to_decl_variable_exp(variable: String, tree: Expression): Tree = {
    new AssignmentExpression(new VariableDecl(variable, Modifiers.Const, null, 0), tree, 0)
  }

  //
  private def create_string_literal(literal: String): LiteralExpression = 
    LiteralExpression.ofStringify(literal, LiteralExpression.Kind.String)

  // generated expression will be like => __export = {a:""}
  private def assign_extract_export_literal_to_export(obj_literal: VmyObject) =
    new AssignmentExpression(new IdExpr(0, Tree.Tag.Id, "__export"), obj_literal, 0)

  //extract all export as __export's elements, and transform to object literal, assign to __export variable when it declared
  private def transform_as_export_pre_declaration(block: BlockStatement):BlockStatement = {
    // new BlockStatement()
    val expression_list = ju.ArrayList[Tree]()
    expression_list.add(assign_extract_export_literal_to_export(extract_export_as_objet_literal(block)))
    expression_list.addAll(block.exprs())
    new BlockStatement(expression_list, block.position())
  }
  private def extract_export_as_objet_literal(block: BlockStatement): VmyObject = {
    def transform_one_export(ex: ExportState): ju.Map[String,Expression] = {
      val one_ex = ex
      val exs = ju.HashMap[String, Expression]()
      def get_export_name(ept: ExportState.ExportExp): String =
        if ept.hasAlias() then ept.alias()
        else ept.name()
      if(one_ex.isOne()){
        val exp = one_ex.getOne();
        ju.Map.of(get_export_name(exp), create_string_literal("predeclaration for export"))
      }
      else if(one_ex.isObjForm()){
        val exps = one_ex.getAll()
        exps.forEach{ e => 
          exs.put(get_export_name(e), create_string_literal("predeclaration for export"))
        }
        exs
      }
      else ju.Map.of()
    }
    val obj_literal: ju.Map[String,Expression] = ju.HashMap() 
    block.exprs().forEach{ expression => 
      expression match
        case ex: ExportState => obj_literal.putAll(transform_one_export(ex))
        case _ =>
    }
    new VmyObject(obj_literal, Tree.Tag.Obj, 0)
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
    unit match {
      case root: RootCompileUnit => 
        root.body match {
          case _fn @ CompiledFn(name, fn_params, fn_rtype, fn_block, ups, pos) if name == "main" => {
            val fn = _fn.asInstanceOf[CompiledFn]
            if(!fn.compiled) {
              fn.compileFinish()
              val mainFnCall = CallExpr( -1, null, "main", new ListExpr(-1, null, java.util.List.of())) 
              val block_elems = new ArrayList[Tree](imports.size() + exports.size() + 2)
              // block_elems.addAll(imports)
              block_elems.add(decl_export_variable())
              block_elems.add(decl_import_variable())
              block_elems.add(new RegisterModuleTree())
              block_elems.addAll(ju.List.of(_fn, mainFnCall))
              // block_elems.addAll(exports)
              // block_elems.addAll(transform_export(exports))
              new RootCompileUnit(
                new BlockStatement(block_elems, -1),-1)
                .setFile(root.file_name())
            }
            else unit
          }
          case _ => unit
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

  // in vmy , there is a variable exists in global called __G
  // it's a const value and obj 
  // all vmy module will be in __G
  // for example:
  // there is a module named "C:/class.vmy"
  // we can looking it by call : val m = __G("C:/class.vmy")
  // At end of each file exec, we will manually register it all export as an obj in __G named by it's absolute path name
  // At begin of each file exec, we will manually register a variable name called __export
  // when executing the file code, we will put every export in __export's member
  //
  // At begin of each file exec, we will register a variable named __import,
  // all import obj will be as its member
  // and we will transform each import as an import and a list of variable decl which value is 
  // the member of __import's member named the module
  // for each import , we will create a new variable to store all export in that module
  private def decl_export_variable(): Tree = const_object_variable_decl("__export")
  private def decl_import_variable(): Tree = const_object_variable_decl("__import")

  private def const_object_variable_decl(name: String): AssignmentExpression = {
    new AssignmentExpression(new VariableDecl(name, Modifiers.Empty, null, 0), new VmyObject(ju.HashMap(), 0), 0)
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
            .setFile(fn.file_name())
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

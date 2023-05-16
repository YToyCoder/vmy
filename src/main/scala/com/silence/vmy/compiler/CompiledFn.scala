package com.silence.vmy.compiler

import com.silence.vmy.shared.Scope
import com.silence.vmy.compiler.tree._
import com.silence.vmy.compiler.Compilers.CompileUnit
import com.silence.vmy.compiler.tree.Tree.Tag
import com.silence.vmy.shared.EmulatingValue
import com.silence.vmy.evaluate._ 
import com.silence.vmy.shared.EmulatingValue.BaseEV
import com.silence.vmy.shared.EmulatingValue.valueType

import EmulatingValue.{EVEmpty, EVFunction, EVList, EVObj, Zero}

import scala.annotation.tailrec
import java.{util => ju}

case class UpValue(private val n: String, private val s: Scope) extends BaseEV
{

  override def value: valueType = 
    variable_value match
      case None => EVEmpty
      case Some(value) => value.value

  override def name = n
  def variable_value = 
    s.lookup(n)
  override def update(newValue: EmulatingValue): EmulatingValue = {
    variable_value match 
    {
      case None => EVEmpty
      case Some(variable) => 
        variable() = newValue
    }
  }

}

class UpValues(private val uvs: Array[UpValue]) 
{
  def find(name: String): Option[UpValue] = 
  {
    if(uvs == null) None
    else 
      val size = uvs.length
      @tailrec
      def doFind(loc: Int): Option[UpValue] = {
        if(loc >= size) None
        else if(uvs(loc) != null && uvs(loc).name == name) 
          Some(uvs(loc))
        else doFind(loc + 1)
      }
      uvs match {
        case null => None
        case _    => doFind(0)
      }
  }

  override def toString(): String = 
    uvs match
      case null => "null"
      case _ => uvs.mkString

}
object UpValues 
{
  def apply(uvs: Array[UpValue]) = new UpValues(uvs)
}

case class CompiledFn(
  val name: String, 
  val params: java.util.List[VariableDecl], 
  val ret: TypeExpr, 
  val body: BlockStatement,
  val upvalues: UpValues,
  val position: Long) 
  extends FunctionDecl 
  with CompileUnit
{
  private var f: String = ""
  def setFile(_f:String) = 
    f = _f
    this
  override def file_name() = f
  def tag() = Tag.Fun
  var compiledFlag = false
  override def compiled() = compiledFlag
  override def node() = this
  override def accept[R,T](visitor: TreeVisitor[R,T], payload: T): R = 
    visitor.visitFunctionDecl(this, payload)
  override def accept[T](visitor: TVisitor[T], t :T): Tree =
    if(visitor.enterFunctionDecl(this, t))
      visitor.leaveFunctionDecl(setBody(body.accept(visitor, t).asInstanceOf[BlockStatement]), t)
    else this
  private def setBody(_body: BlockStatement): FunctionDecl = 
    if(_body == body()) this
    else new CompiledFn(name, params, ret, _body, upvalues, position)

  override def toString(): String = s">> compiled fun << \n${fnDeclToString(this)}" 
  def compileFinish(): Unit = compiledFlag = true
}

object CompileUnit 
{
  def wrapAsCompileUnit(node: Tree): CompileUnit = 
    node match 
      case r: Root =>  
        new RootCompileUnit(r.body(), -1)
        .setFile(r.file_name())
      case _ => wrapAsCompiledFn(node)

  def wrapAsCompiledFn(node: Tree) = 
  {
    node match 
    {
      case fn: CompiledFn => fn
      case fn: FunctionDecl => 
      {
        val fndecl = fn.asInstanceOf[FunctionDecl]
        new CompiledFn( 
          fndecl.name, fndecl.params, 
          fndecl.ret, fndecl.body, 
          null, fndecl.position)
      }
      case r: Root => 
        val fndecl = r.asInstanceOf[Root]
        new CompiledFn( 
          "main", java.util.List.of(), 
          null, fndecl.body.asInstanceOf[BlockStatement], 
          null, fndecl.position)
          .setFile(r.file_name())
      case _ => null
    }
  }
}

class RootCompileUnit(
  val body: Tree, 
  val position: Long, 
  private val _i: ju.List[ImportState] = ju.List.of(),
  private val _e: ju.List[ExportState] = ju.List.of(),
  private var compiledFlag: Boolean = false)
  extends Root
  with CompileUnit
{ 
  private var _f : String = ""
  def setFile(file : String ) = 
    _f = file
    this
  override def file_name(): String =  _f
  override def tag(): Tag = Tag.Root
  override def node(): Tree = this
  override def compiled(): Boolean = compiledFlag
  def compileFinish(): Unit = compiledFlag = true
  override def exports(): ju.List[ExportState] = _e
  override def imports(): ju.List[ImportState] = _i

  override def accept[T](visitor: TVisitor[T], t: T): Tree = 
    if(visitor.enterRoot(this, t))
      visitor.leaveRoot(setBody(body.accept(visitor, t)), t)
    else this

  private def setBody(_body: Tree): RootCompileUnit = 
    if(_body == body) this
    else new RootCompileUnit(_body, position, imports(), exports(), compiledFlag)

  override def accept[R, T](visitor: TreeVisitor[R, T], payload: T): R = 
    visitor.visitRoot(this, payload)
  override def toString(): String = s"root > \n ${ body.toString() }"
}

trait PerCompileUnitTVisitor extends TVisitor[CompileContext]
{
  type ContextType = CompileContext
  override def enterFunctionDecl(fn: FunctionDecl, context: CompileContext) = false 

  private def defaultVisit() = {}

  def enterVisit(context: CompileContext, unit: CompileUnit): CompileUnit= 
    unit
  def leaveVisit(context: CompileContext, unit: CompileUnit): CompileUnit=
    unit

}

class CompileContext extends EmulatorContext 
{
  private var currentVisitor: PerCompileUnitTVisitor = _
  def setCurrentVisitor(visitor: PerCompileUnitTVisitor) = 
    currentVisitor = visitor
  def getCurrentVisitor = currentVisitor
}

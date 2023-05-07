package com.silence.vmy.compiler

import com.silence.vmy.shared.Scope
import com.silence.vmy.compiler.tree._
import com.silence.vmy.compiler.Compilers.CompileUnit
import com.silence.vmy.compiler.tree.Tree.Tag
import com.silence.vmy.shared.EmulatingValue
import com.silence.vmy.evaluate._

import EmulatingValue.{EVEmpty, EVFunction, EVList, EVObj, Zero}

import scala.annotation.tailrec

class UpValue(private val n: String, private val s: Scope) 
{
  def name = n
  def scope = s
  def variable_value = 
    s.lookup(n)
  def update(newValue: EmulatingValue): EmulatingValue = {
    variable_value match 
    {
      case None => EVEmpty
      case Some(variable) => 
        variable() = newValue
    }
  }

}

class UpValues(private val uvs: UpValue*) 
{
  def find(name: String): Option[UpValue] = 
  {
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
}

class CompiledFn(
  val name: String, 
  val params: java.util.List[VariableDecl], 
  val ret: TypeExpr, 
  val body: BlockStatement,
  val upvalues: UpValues,
  val position: Long) 
  extends FunctionDecl 
  with CompileUnit
{
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

  override def toString(): String = fnDeclToString(this)
}

object CompileUnit 
{
  def wrapAsCompileUnit(node: Tree): CompileUnit = 
  {
    node match 
    {
      case e: CompiledFn => e
      case e: FunctionDecl => 
      {
        val fndecl = e.asInstanceOf[FunctionDecl]
        new CompiledFn( 
          fndecl.name, fndecl.params, 
          fndecl.ret, fndecl.body, 
          null, fndecl.position)
      }
      case e: Root => 
        val fndecl = e.asInstanceOf[Root]
        new CompiledFn( 
          "main", java.util.List.of(), 
          null, fndecl.body.asInstanceOf[BlockStatement], 
          null, fndecl.position)
      case _ => null
    }
  }
}

trait PerCompileUnitTVisitor extends TVisitor[CompileContext]
{
  type ContextType = CompileContext
  override def enterFunctionDecl(fn: FunctionDecl, context: CompileContext) = 
    context.getCurrentVisitor != this
}

class CompileContext extends EmulatorContext 
{
  private var currentVisitor: PerCompileUnitTVisitor = _
  def setCurrentVisitor(visitor: PerCompileUnitTVisitor) = 
    currentVisitor = visitor
  def getCurrentVisitor = currentVisitor
}

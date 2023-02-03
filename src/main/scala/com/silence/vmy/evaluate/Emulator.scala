package com.silence.vmy.evaluate

import com.silence.vmy.compiler.tree.Tree.Tag
import com.silence.vmy.compiler.tree.{AssignmentExpression, BinaryOperateExpression, BlockStatement, CallExpr, FunctionDecl, ListExpr, LiteralExpression, ReturnExpr, Root, TreeVisitor, TypeExpr, Unary, VariableDecl}
import com.silence.vmy.evaluate.EmulatingValue.RetValue

import java.util.Objects

sealed trait EmulatingValue{
  def value: Any
}

object EmulatingValue {
  def apply(value: Any): EmulatingValue =
    value match {
      case e : Int => EVInt(e)
      case e : Double => EVDouble(e)
      case e : Long => EVLong(e)
      case e : String => EVString(e)
      case e : Boolean => EVBool(e)
      case e : EmulatingValue => RetValue(e)
      case e : FunctionDecl => EVFunction(e)
    }

  case class EVLong(value: Long) extends EmulatingValue
  case class EVInt(value: Int) extends  EmulatingValue
  case class EVDouble(value: Double) extends  EmulatingValue
  case class EVString(value: String) extends EmulatingValue
  case class EVBool(value: Boolean) extends EmulatingValue
  case class EVFunction(value: FunctionDecl) extends EmulatingValue
  case class RetValue(value: EmulatingValue) extends EmulatingValue
  object EVEmpty extends EmulatingValue {
    override def value: Any = throw new Exception("EVEmpty!")
  }


  def reverse(v: EmulatingValue): EmulatingValue =
    v.value match {
      case e : Int => EmulatingValue(-e)
      case e : Double=> EmulatingValue(-e)
      case e : String=> EmulatingValue("-" + e)
      case e : Boolean => EmulatingValue(!e)
    }

  def add(a: EmulatingValue, b: EmulatingValue) = {
  }
}

trait Emulator {
  def run(): EmulatingValue
}

import scala.collection.mutable

class TreeEmulator extends TreeVisitor[EmulatingValue, EmulatingValue] with Emulator {
  class Scope(protected val parent: Scope) {
    def lookup(name: String): EmulatingValue = {
      var looking: EmulatingValue = null
      if(Objects.nonNull(parent)){
        looking = parent.lookup(name)
        if(looking != EmulatingValue.EVEmpty)
          return looking
      }
      locals.getOrElse(name, EmulatingValue.EVEmpty)
    }
    def put(name: String, v: EmulatingValue) = locals.put(name, v)
    private val locals: mutable.Map[String, EmulatingValue] = mutable.Map()
  }

  class FunctionScope(parent: Scope) extends Scope(parent) {
  }

  class Frame(parent: Frame = null) extends FunctionScope(parent){
    def last: Frame = super.parent.asInstanceOf[Frame]
  }

  private var frame: Frame = null

  override def visitLiteral(expression: LiteralExpression, payload: EmulatingValue): EmulatingValue = {
    val literal = expression.literal().asInstanceOf[String]
    if(expression.isInt) EmulatingValue(literal.toInt)
    else if(expression.isBoolean) EmulatingValue(literal match {
      case "true" => true
      case "false" => false
      case _ => throw new Exception()
    })
    else if(expression.isDouble) EmulatingValue(literal.toDouble)
    else if(expression.isString) EmulatingValue(literal)
    else throw new Exception("error in visiting literal")
  }

  override def visitUnary(expression: Unary, payload: EmulatingValue): EmulatingValue =
    expression.tag() match {
      case Tag.Add => expression.body().accept(this, payload)
      case Tag.Sub => EmulatingValue.reverse(expression.body().accept(this, payload))
      case _ => throw new Exception(s"error${expression.position()}")
    }

  override def visitBlock(statement: BlockStatement, payload: EmulatingValue): EmulatingValue = {
    val expressions = statement.exprs()
    def visitEach(i : Int): EmulatingValue = {
      if(i >= expressions.size()) null
      else {
        val el = expressions.get(i)
        val v = el.accept(this, payload)
        if(v.isInstanceOf[RetValue]) v
        else visitEach(i + 1)
      }
    }
    visitEach(0)
  }

  override def visitBinary(expression: BinaryOperateExpression, payload: EmulatingValue): EmulatingValue = {
    val left = expression.left()
    val right= expression.right()
    expression.tag() match {
      case Tag.Add => left.accept(this, payload)
    }
    null
  }

  override def visitVariableDecl(expression: VariableDecl, payload: EmulatingValue): EmulatingValue = ???

  override def visitAssignment(expression: AssignmentExpression, payload: EmulatingValue): EmulatingValue = ???

  override def visitFunctionDecl(function: FunctionDecl, payload: EmulatingValue): EmulatingValue = ???

  override def visitRoot(root: Root, payload: EmulatingValue): EmulatingValue = {
    frame = new Frame()
    val ret = root.body().accept(this, payload)
    if(ret == EmulatingValue.EVEmpty) EmulatingValue.EVEmpty
    else ret
  }

  override def visitListExpr(expr: ListExpr, payload: EmulatingValue): EmulatingValue =
    ???

  override def visitReturnExpr(expr: ReturnExpr, payload: EmulatingValue): EmulatingValue = ???

  override def visitTypeExpr(expr: TypeExpr, payload: EmulatingValue): EmulatingValue = ???

  override def visitCallExpr(expr: CallExpr, payload: EmulatingValue): EmulatingValue = ???

  override def run(): EmulatingValue = ???
}
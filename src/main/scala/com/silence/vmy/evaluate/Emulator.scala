package com.silence.vmy.evaluate

import com.silence.vmy.compiler.tree.Tree.Tag
import com.silence.vmy.compiler.tree.{AssignmentExpression, BinaryOperateExpression, BlockStatement, CallExpr, FunctionDecl, ListExpr, LiteralExpression, ReturnExpr, Root, TreeVisitor, TypeExpr, Unary, VariableDecl}
import com.silence.vmy.evaluate
import com.silence.vmy.evaluate.EmulatingValue.{RetValue, initValue}
import com.silence.vmy.compiler.{BuiltinTypeString, Modifiers}

import java.util.Objects
import scala.annotation.{implicitAmbiguous, tailrec, targetName}

trait EmulatingValue{
  def value: EmulatingValue.valueType
  def name: String
  def update(v: EmulatingValue) : EmulatingValue
  @targetName("add")
  def +(other: EmulatingValue): EmulatingValue
  @targetName("sub")
  def -(other: EmulatingValue): EmulatingValue
  @targetName("multi")
  def *(other: EmulatingValue): EmulatingValue
  @targetName("div")
  def /(other: EmulatingValue):  EmulatingValue
}


trait Emulator {
  def run(): EmulatingValue
}

import scala.collection.mutable

object TreeEmulator {
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

    def put(name: String, v: EmulatingValue): Option[EmulatingValue] = locals.put(name, v)

    def update(name: String, v: EmulatingValue): Option[EmulatingValue] = {
      if(!exists(name))
        return None
      @tailrec
      def do_update(id: String, cur: TreeEmulator.Scope): Option[EmulatingValue] = {
        if(cur == null) None
        else if(locals.exists((n, _) => n == id)) locals.put(id, v)
        else do_update(id, cur.parent)
      }
      do_update(name, this)
    }

    @tailrec
    final def exists(name: String): Boolean = {
      if(locals.exists((n, _) => n == name)) true
      else if(parent == null) false
      else parent.exists(name)
    }

    private val locals: mutable.Map[String, EmulatingValue] = mutable.Map()
  }

  class FunctionScope(parent: Scope) extends Scope(parent)

  class Frame(parent: Frame = null) extends FunctionScope(parent){
    def last: Frame = parent
  }
}

object EmulatingValue {
  type valueType = Int | Double | Long | String | Boolean | EmulatingValue | FunctionDecl
  def apply(value: valueType): EmulatingValue = apply(value, null)

  def apply(value: valueType, name: String, mutable: Boolean): EmulatingValue =
    val ret = value match {
      case e : Int => EVInt(e)
      case e : Double => EVDouble(e)
      case e : Long => EVLong(e)
      case e : String => EVString(e)
      case e : Boolean => EVBool(e)
      case e : EmulatingValue => RetValue(e)
      case e : FunctionDecl => EVFunction(e)
    }
    ret.setName(name)
    if(mutable) ret else ret.immutable()

  def initValue(id: String): valueType =
    id match {
      case BuiltinTypeString.IntT => 0
      case BuiltinTypeString.StrT => null.asInstanceOf[String]
      case BuiltinTypeString.DoubleT => 0.0
      case BuiltinTypeString.BoolT => true
      case BuiltinTypeString.LongT => 0L
    }

  def apply(value: valueType, name: String): EmulatingValue =
    apply(value, name, true)

  abstract class BaseEV extends EmulatingValue{
    private var n: String = _
    private var scope: TreeEmulator.Scope = _
    private var updatable: Boolean = true
    def immutable(): BaseEV =
      updatable = false
      this

    def setName(_n: String) : BaseEV =
      n = _n
      this

    def name: String = n

    override def update(v: EmulatingValue): EmulatingValue = {
      if(scope == null) {
        println(s"no scope for variable ${name}")
        throw new Exception("variable update error")
      }
      val old = scope.lookup(name)
      scope.update(name, v)
      old
    }

    def updateScope(s: TreeEmulator.Scope): TreeEmulator.Scope =
      val old = scope
      scope = s
      old

    @targetName("add")
    override def +(other: EmulatingValue): EmulatingValue = ???
    @targetName("sub")
    override def -(other: EmulatingValue): EmulatingValue = ???
    @targetName("multi")
    override def *(other: EmulatingValue): EmulatingValue = ???
    @targetName("div")
    override def /(other: EmulatingValue):  EmulatingValue = ???
  }

  case class EVLong(value: Long) extends BaseEV
  case class EVInt(value: Int) extends BaseEV
  case class EVDouble(value: Double) extends BaseEV
  case class EVString(value: String) extends BaseEV
  case class EVBool(value: Boolean) extends BaseEV
  case class EVFunction(value: FunctionDecl) extends BaseEV
  case class RetValue(value: EmulatingValue) extends BaseEV
  object EVEmpty extends BaseEV {
    override def value: EmulatingValue.valueType = throw new Exception("EVEmpty!")
  }


  def reverse(v: EmulatingValue): EmulatingValue =
    v.value match {
      case e : Int => EmulatingValue(-e)
      case e : Double=> EmulatingValue(-e)
      case e : String=> EmulatingValue("-" + e)
      case e : Boolean => EmulatingValue(!e)
      case _ => throw new Exception(s"")
    }
}

class TreeEmulator extends TreeVisitor[EmulatingValue, EmulatingValue] with Emulator {

  private var frame: TreeEmulator.Frame = _

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
    @tailrec
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
    val left = expression.left().accept(this, payload)
    val right= expression.right().accept(this, payload)
    expression.tag() match {
      case Tag.Add => left + right
      case Tag.Sub => left - right
      case Tag.Multi => left * right
      case Tag.Div => left / right
      case Tag.AddEqual => {
        val temp = left + right
        left.update(temp)
        temp
      }
      case Tag.SubEqual => {
        val temp = left - right
        left.update(temp)
        temp
      }
      case Tag.MultiEqual => {
        val temp = left * right
        left.update(temp)
        temp
      }
      case Tag.DivEqual => {
        val temp = left / right
        left.update(temp)
        temp
      }
      case _ => throw new Exception("binary error")
    }
  }

  override def visitVariableDecl(expression: VariableDecl, payload: EmulatingValue): EmulatingValue = {
    val name = expression.name()
    val variable_type = expression.t()
    EmulatingValue(initValue(variable_type.typeId()), name, expression.modifiers().is(Modifiers.CVariableConst))
  }

  override def visitAssignment(expression: AssignmentExpression, payload: EmulatingValue): EmulatingValue = {
    null
  }

  override def visitFunctionDecl(function: FunctionDecl, payload: EmulatingValue): EmulatingValue = ???

  override def visitRoot(root: Root, payload: EmulatingValue): EmulatingValue = {
    frame = new TreeEmulator.Frame()
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
package com.silence.vmy.evaluate

import com.silence.vmy.compiler.tree.Tree.Tag
import com.silence.vmy.compiler.tree._
import com.silence.vmy.compiler.{Modifiers, Tokens}
import com.silence.vmy.evaluate.EmulatingValue.{RetValue, initValue}
import com.silence.vmy.runtime.VmyRuntimeException
import math.Fractional.Implicits.infixFractionalOps
import math.Integral.Implicits.infixIntegralOps
import math.Numeric.Implicits.infixNumericOps
import com.silence.vmy.evaluate.EmulatingValue.EVEmpty.mkOrderingOps

import com.silence.vmy.runtime.VmyFunctions

import java.util.List

import scala.annotation.tailrec
import scala.collection.mutable

sealed trait EmulatingValue extends Ordering[EmulatingValue]{
  def value: EmulatingValue.valueType
  def name: String
  def update(v: EmulatingValue) : EmulatingValue
  def +(other: EmulatingValue): EmulatingValue
  def -(other: EmulatingValue): EmulatingValue
  def *(other: EmulatingValue): EmulatingValue
  def /(other: EmulatingValue):  EmulatingValue
  def toBool : Boolean
  def toInt : Int
  def compare(a:EmulatingValue,other: EmulatingValue) = (a- other).toInt
  def copyPropsFrom(other: EmulatingValue) : EmulatingValue
}

object TreeEmulator {
  abstract class Scope(pre: Scope) {
    val variables: mutable.Map[String, EmulatingValue] = mutable.Map()
    def lookup(name: String): Option[EmulatingValue] = {
      val variable = variables.getOrElse(
        name,
        pre match {
          case null => null
          case _ => {
            pre.lookup(name) match {
              case None => null 
              case Some(variable) => variable
            }
          }
        }
      ) 
      variable match {
        case null => None
        case variable => Some(variable.asInstanceOf[EmulatingValue])
      }
    }

    def update(name: String, value: EmulatingValue): Scope = {
      variables(name) = value
      this
    }
  }

  case class Frame(pre: Frame) extends Scope(pre)
}

object EmulatingValue {
  type valueType = PrimaryOpSupportType | String| Boolean | EmulatingValue | FunctionDecl
  type PrimaryOpSupportType = Int | Double | Long 
  def apply(value: valueType): BaseEV = apply(value, null)
  // must value use 
  def apply(value: valueType, name: String): BaseEV = apply(value, name, true)

  // function declaration use
  def apply(value: valueType, name: String, mutable: Boolean): BaseEV = {
    val ret = value match {
      case e: Int => EVInt(e)
      case e: Double => EVDouble(e)
      case e: Long => EVLong(e)
      case e: String => EVString(e)
      case e: Boolean => EVBool(e)
      case e: FunctionDecl => EVFunction(e)
      case e: RetValue => RetValue(e)
      case e: EmulatingValue => apply(e.value)
    }
    ret.setName(name)
    if (mutable) ret else ret.immutable()
  }
  val Zero : EmulatingValue = EVInt(0)

  // get init value for different type value
  def initValue(id: String): valueType = {
    id match {
      case "Int" => 0
      case "String" => ""
      case "Double" => 0.0
      case "Boolean" => true
      case "Long" => 0L
      case _ => 0 // todo
    }
  }

  abstract class BaseEV extends EmulatingValue{
    private var n: String = _
    private var scope: TreeEmulator.Scope = _
    private var updatable: Boolean = true
    def immutable(): BaseEV = {
      updatable = false
      this
    }

    def setName(_n: String) : BaseEV = {
      n = _n
      this
    }
    // override def compare(other: EmulatingValue): Int = (this - other).toInt
    override def copyPropsFrom(other: EmulatingValue): EmulatingValue = {
      n = other.name

      other match {
        case v: BaseEV =>
          scope = v.scope
          updatable = v.updatable
        case _ =>
      }
      this
    }
    override def toString() = value.toString()

    def name: String = n

    override def update(v: EmulatingValue): EmulatingValue = {
      if(scope == null) {
        throw new Exception(s"variable-update-error=>no scope for variable ${name}")
      }
      if(!this.updatable)
        throw new VmyRuntimeException("const variable can't updated")

      scope.lookup(name) match {
        case None => throw new VmyRuntimeException("not in scope")
        case Some(value) => {
          scope(name) = v copyPropsFrom value
          value
        }
      }
    }

    def updateScope(s: TreeEmulator.Scope): TreeEmulator.Scope = {
      val old = scope
      scope = s
      old
    }

    override final def toBool : Boolean = {
      this match {
        case EVLong(value) => value != 0 
        case EVInt(value) => value != 0
        case EVDouble(value) => value != 0
        case EVString(value) => value == "true"
        case EVBool(value) => value
        case EVFunction(value) => value != null
        case EVEmpty => false
      }
    }

    override final def toInt : Int = 
      def boolToInt(boolV : Boolean) : Int = if(boolV) 0 else 1
      this match {
        case EVLong(value) => value.toInt 
        case EVInt(value) => value 
        case EVDouble(value) => value.toInt 
        case EVString(value) => boolToInt(value == null) 
        case EVBool(value) => boolToInt(value)
        case EVFunction(value) => boolToInt(value != null)
      }

    private final def toPrimaryOpType(two: (EmulatingValue.valueType, EmulatingValue.valueType)): (PrimaryOpSupportType, PrimaryOpSupportType) =
      two match {
        case (left : PrimaryOpSupportType, right : PrimaryOpSupportType) => (left, right)
        case _ => throw new VmyRuntimeException(s"should not reach this")
      }
    // private final def doOps[T, X](
    //   two : (PrimaryOpSupportType, PrimaryOpSupportType), 
    //   op : (T, X) => T | X
    // ): PrimaryOpSupportType =  
    //   two match {
    //     case (l: Int, r: Int) => op(l, r)
    //     case (l: Double, r: Int) => op(l, r)
    //     case (l: Int, r: Double) => op(l, r)
    //     case (l: Double, r: Double) => op(l, r)
    //   }
    
//    @targetName("add")
    override def +(other: EmulatingValue): EmulatingValue = {
      @tailrec
      def add (two: (PrimaryOpSupportType, PrimaryOpSupportType)): PrimaryOpSupportType = 
        two match{
          case (l: Int, r: Int) => l + r
          case (l: Double, r: Int) => l + r
          case (l: Double, r: Double) => l + r
          case (l: Long, r: Int) => l + r
          case (l: Long, r: Double) => l + r
          case (l: Long, r: Long) => l + r
          case (l, r)=> add(r, l)
        }
      val addResult : valueType = (this, other) match {
        case (EVFunction(_), _) | (_, EVFunction(_)) => this.toString + other.toString
        case (left, right) => 
          (left.value, right.value) match {
            case (l:String, r) => l + r
            case (l, r:String) => r + l
            case two => {
              val (pl,pr) = toPrimaryOpType(two)  
              add(pl, pr)
            }
          }

      }
      EmulatingValue(addResult)
    }

    //    @targetName("sub")
    override def -(other: EmulatingValue): EmulatingValue = {
      @tailrec
      def sub(two: (PrimaryOpSupportType, PrimaryOpSupportType)): PrimaryOpSupportType = 
        two match {
          case (l: Int, r: Int) => l - r
          case (l: Double, r: Int) => l - r
          case (l: Double, r: Double) => l - r
          case (l: Long, r: Int) => l - r
          case (l: Long, r: Double) => l - r
          case (l: Long, r: Long) => l - r
          case (l, r)=> sub(r, l)
        }
      (this.value, other.value) match {
        case (l: PrimaryOpSupportType, r: PrimaryOpSupportType) => EmulatingValue(sub(l,r))
        case (l, r) => {
          // if(debug) println(s"sub ops l:${l} r:${r}")
          EmulatingValue.EVEmpty
        } 
      }
    }
//    @targetName("multi")
    override def *(other: EmulatingValue): EmulatingValue = null
//    @targetName("div")
    override def /(other: EmulatingValue):  EmulatingValue = null
  }

  private case class EVLong(value: Long) extends BaseEV //{ type ValueType = Long }
  private case class EVInt(value: Int) extends BaseEV //{ type ValueType = Int }
  private case class EVDouble(value: Double) extends BaseEV //{ type ValueType = Double }
  private case class EVString(value: String) extends BaseEV //{ type ValueType = String }
  private case class EVBool(value: Boolean) extends BaseEV //{ type ValueType = Boolean }
  {
    override def toString(): String = if(value) "true" else "false"
  }
  case class EVFunction(value: FunctionDecl) extends BaseEV {}
  case class RetValue(_value: EmulatingValue) extends BaseEV {
    // type ValueType = EmulatingValue
    override def value = _value.value
  }
  object EVEmpty extends BaseEV {
    override def value = null 
    override def toString(): String = "Null" 
      // throw new Exception("EVEmpty!")
  }


  def reverse(v: EmulatingValue): EmulatingValue = {
    v.value match {
      case e : Int => EmulatingValue(-e)
      case e : Double=> EmulatingValue(-e)
      case e : String=> EmulatingValue("-" + e)
      case e : Boolean => EmulatingValue(!e)
      case _ => throw new Exception(s"")
    }
  }
}

class TreeEmulator extends TreeVisitor[EmulatingValue, EmulatingValue] {
  import EmulatingValue.{EVEmpty, EVFunction,Zero}
  val debug: Boolean = false

  private var frame: TreeEmulator.Frame = _
  private def createFrame() : TreeEmulator.Frame = {
    frame = TreeEmulator.Frame(frame)
    frame
  }

  private def declareVariable(name: String, initValue: EmulatingValue.valueType, mutable: Boolean): EmulatingValue = {
    val vv /* variable and value */= EmulatingValue(initValue, name, mutable)
    vv.updateScope(frame)
    frame.update(name, vv)
    vv
  }

  private def declareVariable(name: String, initValue: EmulatingValue.valueType): EmulatingValue = declareVariable(name, initValue, true)

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
      case _ => throw new Exception(s"error${Tokens.stringifyLocation(expression.position())}")
    }

  override def visitBlock(statement: BlockStatement, payload: EmulatingValue): EmulatingValue = {
    val expressions = statement.exprs()
    @tailrec
    def visitEach(i : Int): EmulatingValue = {
      if(i >= expressions.size()) EVEmpty 
      else {
        val el = expressions.get(i)
        val v = el.accept(this, payload)
        if(debug)
          println(s"block-elemtent => ${el} \n|>> is RetValue => ${v.isInstanceOf[RetValue]}")
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
      case Tag.Equal => EmulatingValue(left == right)
      case Tag.NotEqual => EmulatingValue(left != right)
      case Tag.Less => EmulatingValue((left - right) < Zero)  
      case Tag.Greater => EmulatingValue((left - right) > Zero )
      case Tag.Le => EmulatingValue((left - right) <= Zero )
      case op => throw new Exception(s"binary error/ ${Tokens.stringifyLocation(expression.position())} ${op}")
    }
  }

  override def visitVariableDecl(expression: VariableDecl, payload: EmulatingValue): EmulatingValue = {
    val name = expression.name()
    val variable_type = expression.t()
    val initv = payload match {
      case null => if(variable_type == null) "" else variable_type.typeId() 
      case otherwise => payload.value
    }
    declareVariable(
      name,
      initv,
      !expression.modifiers().is(Modifiers.CVariableConst))
  }

  override def visitAssignment(expression: AssignmentExpression, payload: EmulatingValue): EmulatingValue = {
    val value = expression.right().accept(this, payload)
    expression.left().accept(this, value)
  }

  override def visitFunctionDecl(function: FunctionDecl, payload: EmulatingValue): EmulatingValue = {
    val name = function.name()
    declareVariable(name, function)
  }

  override def visitRoot(root: Root, payload: EmulatingValue): EmulatingValue = {
    frame = createFrame()
    root.body().accept(this, payload) match {
      case EVEmpty => EVEmpty
      case otherwise => otherwise
    }
  }

  override def visitListExpr[E <: Expression](expr: ListExpr[E], payload: EmulatingValue): EmulatingValue = {
    expr.body().forEach(el => el.accept(this, payload))
    EVEmpty
  }

  override def visitReturnExpr(expr: ReturnExpr, payload: EmulatingValue): EmulatingValue = expr.body().accept(this, payload)

  override def visitTypeExpr(expr: TypeExpr, payload: EmulatingValue): EmulatingValue = null

  override def visitCallExpr(call: CallExpr, payload: EmulatingValue): EmulatingValue = {
    def paramsEval() = call.params.body.stream()
      .map(_.accept(this, null))
      .toList()
    def list2arr(ls: List[EmulatingValue]): Array[EmulatingValue] = {
      val arr: Array[EmulatingValue] = new Array(ls.size())
      for(i <- 0 until ls.size()){
        arr(i) = ls.get(i)
      }
      arr
    }
    if(debug) println(s"visiting call ${call.callId()}")
    frame.lookup(call.callId()) match {
      // function in frame
      case Some(value) if (value.isInstanceOf[EVFunction]) => {
        createFrame()
        paramsEval()
        value.asInstanceOf[EVFunction].value.accept(this, null)
      } 
      case _ => {
        VmyFunctions.lookupFn(call.callId()) match {
          case Some(fn) => 
            VmyFunctions.runNative(
              fn, 
              list2arr(paramsEval()))
          case None => 
            throw new VmyRuntimeException(s"not exists function : ${call.callId()}")
        }
      } 
    }
  }

  override def visitIdExpr(expr: IdExpr, payload: EmulatingValue): EmulatingValue = {
    val idName = expr.name()
    frame.lookup(idName) match {
      case None => throw new VmyRuntimeException(s"not exists ${idName}");
      case Some(value) => value
    }
  }

  override def visitIfStatement(statement: IfStatement, payload: EmulatingValue): EmulatingValue = {
    val ifStatement = statement.ifStatement()
    ifStatement.condition().accept(this, payload) match {
      case e if e != EVEmpty && e.toBool => 
        return ifStatement.block().accept(this, payload)
      case _ => {
    // if(ifStatement.condition().accept(this, payload).toBool){
    //   return ifStatement.block().accept(this, payload)
    // }
        val (elifResult, content) = doWithElif(statement, payload)
        if(elifResult) 
          return content
        return statement.el() match {
          case null => EmulatingValue.EVEmpty
          case block => block.accept(this, payload)
        }
      }
    }
  }

  private[this] def doWithElif(statement: IfStatement, payload: EmulatingValue): (Boolean, EmulatingValue) = {
    val elifs = statement.elif()
    for(i <- 0 until elifs.size()){
      val oneElif = elifs.get(i)
      if(oneElif.accept(this, payload).toBool){
        return (true, oneElif.accept(this, payload))
      }
    }
    (false, EmulatingValue.EVEmpty)
  }
  override def visitArr(arr: ArrExpression, payload: EmulatingValue) : EmulatingValue = {
    EmulatingValue.EVEmpty
  }
}

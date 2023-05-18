package com.silence.vmy.shared

import math.Fractional.Implicits.infixFractionalOps
import math.Integral.Implicits.infixIntegralOps
import math.Numeric.Implicits.infixNumericOps

import com.silence.vmy.shared.EmulatingValue.EVEmpty.mkOrderingOps
import com.silence.vmy.runtime.VmyRuntimeException
import com.silence.vmy.runtime.VmyFunctions
import com.silence.vmy.compiler.tree._
import com.silence.vmy.compiler.CompiledFn
import com.silence.vmy.compiler.Compiler
import com.silence.vmy.compiler.Compilers.CompileUnit
import com.silence.vmy.compiler.CompileContext
import com.silence.vmy.evaluate.TreeEmulator
import com.silence.vmy.compiler.CompileUnit.wrapAsCompileUnit
import com.silence.vmy.compiler.UpValue

import java.util.List
import java.util.ArrayList
import java.util.HashMap
import java.util.Map
import java.{util as ju}

import scala.annotation.tailrec
import scala.collection.mutable
import com.silence.vmy.evaluate.TreeEmulator.ExportValue

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

object EmulatingValue {
  type valueType = 
    PrimaryOpSupportType | 
    String | 
    Boolean | 
    EmulatingValue | 
    FunctionDecl | 
    ArrayT /* array */ | 
    ObjType
  type PrimaryOpSupportType = Int | Double | Long 
  type ArrayT = List[EmulatingValue]
  type ObjType = Map[String, EmulatingValue]
  def isListOrObj(value: EmulatingValue): Boolean = 
    value match
      case EVList(_) | EVObj(_) | EVGlobal => true
      case _ => false
  def upvalueIsListOrObj(value: UpValue): Boolean = 
    value.variable_value match
      case None => false
      case Some(upv) => isListOrObj(upv)
  def apply(value: valueType): BaseEV = apply(value, null)
  // must value use 
  def apply(value: valueType, name: String): BaseEV = apply(value, name, true)

  // function declaration use
  def apply(value: valueType, name: String, mutable: Boolean): BaseEV = {
    val ret = value match {
      case null => EVEmpty
      case e: Int => EVInt(e)
      case e: Double => EVDouble(e)
      case e: Long => EVLong(e)
      case e: String => EVString(e)
      case e: Boolean => EVBool(e)
      case e: FunctionDecl => EVFunction(e)
      case e: RetValue => RetValue(e)
      case e: ArrayT => EVList(e) 
      case e: ObjType => EVObj(e) 
      case e: EmulatingValue => apply(e.value) // rec
    }
    ret.setName(name) // set name
    if (mutable) ret else ret.immutable() // set mutable
  }
  val Zero : EmulatingValue = EVInt(0)
  def createFromLiteralTree(tree: LiteralExpression): EmulatingValue = 
  {
    val literal = tree.literal().asInstanceOf[String]
    if(tree.isInt) EmulatingValue(literal.toInt)
    else if(tree.isBoolean) EmulatingValue(literal match {
      case "true" => true
      case "false" => false
      case _ => throw new Exception()
    })
    else if(tree.isDouble) EmulatingValue(literal.toDouble)
    else if(tree.isString) EmulatingValue(literal)
    else throw new Exception("error in visiting literal")
  }

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
    protected var _scope: Scope = _
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
      if(other.isInstanceOf[BaseEV]){
        val source = other.asInstanceOf[BaseEV]
        _scope = source._scope
        updatable = source.updatable
      }
      this
    }

    override def toString() = 
      // println(s"is global ${this == EVGlobal} call toString")
      value.toString()
    def name: String = n

    override def update(v: EmulatingValue): EmulatingValue = {
      if(_scope == null) {
        throw new Exception(s"variable-update-error=>no scope for variable ${name}")
      }
      if(!this.updatable)
        throw new VmyRuntimeException(s"const variable (${name}) can't updated")

      _scope.lookup(name) match {
        case None => throw new VmyRuntimeException("not in scope")
        case Some(value) => {
          _scope(name) = v copyPropsFrom value
          v
        }
      }
    }

    def updateScope(s: Scope): Scope = {
      val old = _scope
      _scope = s
      old
    }

    override final def toBool : Boolean = {
      this match {
        case EVLong(value) => value != 0 
        case EVInt(value) => value != 0
        case EVDouble(value) => value != 0
        case EVString(value) => value != null
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
      val addResult : valueType = (this.value, other.value) match {
        case (l: PrimaryOpSupportType, r: PrimaryOpSupportType) => add((l,r)) 
        case (value: ArrayT, _) => { 
          value.add(other)
          this
        }
        case (l, r) => this.toString + other.toString
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
        case (l, r) => EmulatingValue.EVEmpty
      }
    }
//    @targetName("multi")
    override def *(other: EmulatingValue): EmulatingValue = 
      @tailrec
      def multi(two: (PrimaryOpSupportType, PrimaryOpSupportType)): PrimaryOpSupportType = 
        two match {
          case (l: Int, r: Int) => l * r
          case (l: Double, r: Int) => l * r
          case (l: Double, r: Double) => l * r
          case (l: Long, r: Int) => l * r
          case (l: Long, r: Double) => l * r
          case (l: Long, r: Long) => l * r
          case (l, r)=> multi(r, l)
        }
      (this.value, other.value) match {
        case (l: PrimaryOpSupportType, r: PrimaryOpSupportType) => EmulatingValue(multi(l,r))
        case (l, r) => EmulatingValue.EVEmpty
      }
//    @targetName("div")
    override def /(other: EmulatingValue):  EmulatingValue = 
      @tailrec
      def div(two: (PrimaryOpSupportType, PrimaryOpSupportType)): PrimaryOpSupportType = 
        two match {
          case (l: Int, r: Int) => l / r
          case (l: Double, r: Int) => l / r
          case (l: Double, r: Double) => l / r
          case (l: Long, r: Int) => l / r
          case (l: Long, r: Double) => l / r
          case (l: Long, r: Long) => l / r
          case (l, r)=> div(r, l)
        }
      (this.value, other.value) match {
        case (l: PrimaryOpSupportType, r: PrimaryOpSupportType) => EmulatingValue(div(l,r))
        case (l, r) => EmulatingValue.EVEmpty
      }
  }

  case class EVLong(value: Long) extends BaseEV //{ type ValueType = Long }
  case class EVInt(value: Int) extends BaseEV //{ type ValueType = Int }
  case class EVDouble(value: Double) extends BaseEV //{ type ValueType = Double }
  case class EVString(value: String) extends BaseEV //{ type ValueType = String }
  case class EVBool(value: Boolean) extends BaseEV //{ type ValueType = Boolean }
  {
    override def toString(): String = if(value) "true" else "false"
  }
  case class EVFunction(value: FunctionDecl) extends BaseEV {
    override def toString(): String = 
      s"Fn(${value.name}) => ${if(value.ret == null) "?" else value.ret.typeId}"
    def compiled = 
      value match 
      {
        case fn: CompiledFn => (fn.asInstanceOf[CompiledFn]).compiled
        case _ => false
      }
  }
  case class RetValue(_value: EmulatingValue) extends BaseEV {
    // type ValueType = EmulatingValue
    override def value = _value.value
  }
  case class EVList(value: ArrayT) extends BaseEV {
    override def toString() = s"[${value.stream().map(_.toString).reduce(_ + "," + _).orElse("")}]"
  }

  case class EVObj(_map: ObjType) extends BaseEV {
    override def value: ObjType = _map
  }
  object EVEmpty extends BaseEV {
    override def value = null 
    override def toString(): String = "Null" 
  }
  class GlobalMap extends HashMap[String, EmulatingValue] {
    override def get(key: Object): EmulatingValue = 
      if(key == "__G") EVGlobal
      else super.get(key)

    override def put(key: String, _v: EmulatingValue) = 
      if(key == "__G")  EVGlobal
      else super.put(key, _v)
  }

  object EVGlobal extends EVObj(new GlobalMap) {

    override def update(v: EmulatingValue): EmulatingValue = 
      this

    override def toString(): String = 
      println("call global toString")
      value.toString()
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

class Scope(pre: Scope) {
  val variables: mutable.Map[String, EmulatingValue] = mutable.Map()
  def preOne: Scope = pre
  def lookup(name: String): Option[EmulatingValue] = {
    val variable: EmulatingValue = variables.getOrElse(
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
      case variable => Some(variable)
    }
  }

  def wrapAsUpValue(name: String): Option[UpValue] =
  {
    variables.get(name) match {
      case None if pre != null => pre.wrapAsUpValue(name)
      case Some(value) => Some(new UpValue(name, this))
      case _ => None
    }
  }

  def update(name: String, value: EmulatingValue): Scope = {
    variables(name) = value
    this
  }

  protected def lookupInScope(name: String): Option[Scope] = {
    variables.get(name) match
      case None => 
        pre match
          case null => None
          case _ => pre.lookupInScope(name)
      case Some(value) => Some(this)
  }

  override def toString(): String = variables.mkString
}

package com.silence.vmy.compiler

import com.silence.vmy.compiler.tree._
import com.silence.vmy.compiler.Context

import scala.collection.mutable.Stack
import scala.collection.mutable

sealed class TheType(val name:String) extends CompilingPhaseType

object StringT extends TheType("string")
object DoubleT extends TheType("double")
object IntT extends TheType("int")
case class FunT(val paramTs: Array[TheType], val ret: TheType) extends TheType("fun")
case class ArrT(val typeVar: TheType)
case class ReType(realType: TheType) extends TheType(s"ret => ${realType.name}")
object NullExistType extends TheType("TypeNotExist")
object VoidT extends TheType("void")

object BuiltinTypeString {
  val StrT = "String"
  val DoubleT = "Double"
  val IntT = "Int"
  val BooleanT = "Boolean"
  val LongT = "Long"
}

case class TypeContext(pre: TypeContext) {
  private val stack : Stack[TheType] = Stack()
  // when lookup failed , it should return NullExistType
  private val map : mutable.Map[String, TheType] = mutable.Map().withDefault(_ => NullExistType)
  def push(t: TheType): TypeContext = {
    stack.push(t) 
    this
  } 
  def pop(): TheType = stack.pop()
  def put(name: String, t: TheType): TypeContext = {
    map(name) = t
    this
  }
  def lookup(name: String): TheType = map(name)
  def peek(): TheType = stack.head
}


class TypeChecker extends TVisitor[Context] {
  private val TypeOrderMap: Map[TheType, Int] = 
    Map((IntT, 0), (DoubleT, 1), (StringT, 2)).withDefault(_ => -1)
  private var typeContextFrame : TypeContext = null
  private def createFrame(): TypeChecker = {
    typeContextFrame = TypeContext(typeContextFrame) 
    this
  }
  private def exitFrame(): TypeChecker = {
    if(
      typeContextFrame != null && 
      typeContextFrame.pre != null
    ) {
      typeContextFrame = typeContextFrame.pre
    }
    this
  }

  private def popType(): TheType = typeContextFrame.pop()
  private def pushType(t: TheType) = {
    typeContextFrame.push(t)
    this
  }
  private def peekType(): TheType = typeContextFrame.peek()

  override def leaveIdExpr(exp: IdExpr, i: Context) = {
    pushType{ typeContextFrame.lookup(exp.name()) }
    exp
  }

  override def leaveUnary(exp: Unary, i: Context) = {
    val body = exp.body.accept(this, i)
    val bodyType = popType()
    pushType(bodyType)
    exp
  }

  private def biggerType(a: TheType, b: TheType): TheType = {
    if(a.getClass() == b.getClass()) then a
    else
      (TypeOrderMap(a), TypeOrderMap(b)) match{
        case (-1, _) | (_, -1)=> NullExistType
        case (aOrder, bOrder) => if(aOrder > bOrder) a else b
      }
  }

  private def unwrapReturnValue(rvalue: TheType): TheType = {
    rvalue match {
      case ReType(realtype) => realtype
      case _ => rvalue
    }
  }
  override def leaveBlock(state: BlockStatement, t: Context) = {
    val states = state.exprs
    var retType : TheType = peekType()
    for(i <- 0 until states.size()){
      // get each state's type
      states.get(i).accept(this, t)
      val stateType = popType()
      // handle return type
      if(stateType.isInstanceOf[ReType]) {
        val realtype = unwrapReturnValue(stateType)
        retType = retType match {
          case null => // first returned type 
            realtype 
          case _ => 
            biggerType(realtype, retType) match {
              case NullExistType => 
              {
                println(s"return type error: type ${realtype} not match ${retType}")
                retType
              }
              case betterType =>  betterType
            }
        }
      }
    }
    retType match {
      case null => pushType(VoidT)
      case _    => pushType(retType)
    }
    state
  }

  override def leaveBinary(exp: BinaryOperateExpression, t : Context) = {
    val oLeft = exp.left()
    val cLeft = oLeft.accept(this, t)
    val lType = popType()
    val oRight = exp.right()
    val cRight = oRight.accept(this, t)
    val rType = popType()
    // ---- 
    // todo
    pushType(biggerType(lType, rType))
    exp
  }

  private def variableDeclType(exp: VariableDecl) : TheType = 
  {
    exp.t match 
    {
      case null => NullExistType
      case t => 
        t.typeId() match 
        {
          case "String" => StringT
          case "Double" => DoubleT
          case "Int" => IntT
          case "void" => VoidT
          case _ => NullExistType
        }
    }
  }

  override def leaveVariableDecl(exp: VariableDecl, t: Context) = 
  {
    // decl must followed with
    // peek to find 
    peekType() match 
    {
      case NullExistType | null => 
        pushType(variableDeclType(exp))
        exp
      case valueType if exp.t() == null => 
        pushType(valueType)
        new VariableDecl(
          exp.name, 
          exp.modifiers, 
          new TypeExpr(-1, null, valueType.toString), 
          exp.position)
        exp
      case valueType => 
        pushType(variableDeclType(exp))
        exp
    }
  }

}

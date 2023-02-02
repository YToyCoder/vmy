package com.silence.vmy.evaluate

import com.silence.vmy.compiler.tree.{AssignmentExpression, BinaryOperateExpression, BlockStatement, CallExpr, FunctionDecl, ListExpr, LiteralExpression, ReturnExpr, Root, TreeVisitor, TypeExpr, Unary, VariableDecl}

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
    }

  case class EVLong(val value: Long) extends EmulatingValue
  case class EVInt(val value: Int) extends  EmulatingValue
  case class EVDouble(val value: Double) extends  EmulatingValue
  case class EVString(val value: String) extends EmulatingValue
  case class EVBool(val value: Boolean) extends EmulatingValue
}

trait Emulator {
  def run(): EmulatingValue
}


class TreeEmulator extends TreeVisitor[EmulatingValue, EmulatingValue] with Emulator {

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
    ???

  override def visitBlock(statement: BlockStatement, payload: EmulatingValue): EmulatingValue = ???

  override def visitBinary(expression: BinaryOperateExpression, payload: EmulatingValue): EmulatingValue = ???

  override def visitVariableDecl(expression: VariableDecl, payload: EmulatingValue): EmulatingValue = ???

  override def visitAssignment(expression: AssignmentExpression, payload: EmulatingValue): EmulatingValue = ???

  override def visitFunctionDecl(function: FunctionDecl, payload: EmulatingValue): EmulatingValue = ???

  override def visitRoot(root: Root, payload: EmulatingValue): EmulatingValue = ???

  override def visitListExpr(expr: ListExpr, payload: EmulatingValue): EmulatingValue = ???

  override def visitReturnExpr(expr: ReturnExpr, payload: EmulatingValue): EmulatingValue = ???

  override def visitTypeExpr(expr: TypeExpr, payload: EmulatingValue): EmulatingValue = ???

  override def visitCallExpr(expr: CallExpr, payload: EmulatingValue): EmulatingValue = ???

  override def run(): EmulatingValue = ???
}
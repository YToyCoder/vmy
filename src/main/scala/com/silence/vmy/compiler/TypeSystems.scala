package com.silence.vmy.compiler

import com.silence.vmy.compiler.tree._
// {
  // AssignmentExpression, 
  // BinaryOperateExpression, 
  // BlockStatement, 
  // CallExpr, 
  // Expression, 
  // FunctionDecl, 
  // IdExpr, 
  // IfStatement, 
  // ListExpr, 
  // LiteralExpression, 
  // ReturnExpr, 
  // Root, 
  // Tree, 
  // TreeVisitor, 
  // TypeExpr, 
  // Unary, 
  // VariableDecl,
  // ArrExpression
// }

sealed class TheType(val name:String) extends CompilingPhaseType

object StringT extends TheType("string")
object DoubleT extends TheType("double")
object IntT extends TheType("int")
case class FunT(val paramTs: Array[TheType], val ret: TheType) extends TheType("fun")
case class NamedT(override val name: String) extends TheType(name)

object BuiltinTypeString {
  val StrT = "string"
  val DoubleT = "double"
  val IntT = "int"
  val BooleanT = "boolean"
  val LongT = "long"
}

class TypeCheck extends TreeVisitor[CompilingPhaseType, CompilingPhaseType]{

  override def visitLiteral(expression: LiteralExpression, payload: CompilingPhaseType): CompilingPhaseType = {
    expression.tag() match {
      case Tree.Tag.DoubleLiteral =>   DoubleT
      case Tree.Tag.StringLiteral =>   StringT
      case Tree.Tag.IntLiteral =>      IntT
      case Tree.Tag.FunctionLiteral => null // todo
      case _ => null
    }
  }

  override def visitUnary(expression: Unary, payload: CompilingPhaseType): CompilingPhaseType =
    expression.body().accept(this, null)

  override def visitBlock(statement: BlockStatement, payload: CompilingPhaseType): CompilingPhaseType = null

  override def visitBinary(expression: BinaryOperateExpression, payload: CompilingPhaseType): CompilingPhaseType = {
    // visit both and compare
    val lTp = expression.left().accept(this, payload)
    val rTp = expression.right().accept(this, payload)
    null
  }

  override def visitForStatement(statement: ForStatement, payload: CompilingPhaseType): CompilingPhaseType= {
    null
  }

  private def expressionAsType(expr: TypeExpr) : CompilingPhaseType = {
    expr.typeId() match {
      case BuiltinTypeString.StrT => StringT
      case BuiltinTypeString.DoubleT => DoubleT
      case BuiltinTypeString.IntT => IntT
      case id => NamedT(id)
    }
  }

  override def visitVariableDecl(expression: VariableDecl, payload: CompilingPhaseType): CompilingPhaseType = expressionAsType(expression.t())

  override def visitAssignment(expression: AssignmentExpression, payload: CompilingPhaseType): CompilingPhaseType = {
    val variable = expression.left().accept(this, payload)
    val expr     = expression.right().accept(this, payload)
    null
  }
  override def visitFunctionDecl(function: FunctionDecl, payload: CompilingPhaseType): CompilingPhaseType = null
  override def visitRoot(root: Root, payload: CompilingPhaseType): CompilingPhaseType = null
  override def visitListExpr[E <: Expression](expr: ListExpr[E], payload: CompilingPhaseType): CompilingPhaseType = null
  override def visitReturnExpr(expr: ReturnExpr, payload: CompilingPhaseType): CompilingPhaseType = null
  override def visitTypeExpr(expr: TypeExpr, payload: CompilingPhaseType): CompilingPhaseType = null
  override def visitCallExpr(expr: CallExpr, payload: CompilingPhaseType): CompilingPhaseType = null
  override def visitIdExpr(expr: IdExpr, payload: CompilingPhaseType): CompilingPhaseType = null
  override def visitIfStatement(statement: IfStatement, payload: CompilingPhaseType): CompilingPhaseType = null
  override def visitArr(expr: ArrExpression, payload: CompilingPhaseType): CompilingPhaseType = null
}

object TypeCheck {
  def apply(): TypeCheck = new TypeCheck()
}

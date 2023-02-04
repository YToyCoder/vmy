package com.silence.vmy.compiler

import com.silence.vmy.compiler.tree.{AssignmentExpression, BinaryOperateExpression, BlockStatement, CallExpr, Expression, FunctionDecl, ListExpr, LiteralExpression, ReturnExpr, Root, Tree, TreeVisitor, TypeExpr, Unary, VariableDecl}

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
    }
  }

  override def visitUnary(expression: Unary, payload: CompilingPhaseType): CompilingPhaseType =
    expression.body().accept(this, null)

  override def visitBlock(statement: BlockStatement, payload: CompilingPhaseType): CompilingPhaseType = ???

  override def visitBinary(expression: BinaryOperateExpression, payload: CompilingPhaseType): CompilingPhaseType = {
    // visit both and compare
    val lTp = expression.left().accept(this, payload)
    val rTp = expression.right().accept(this, payload)
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

  override def visitFunctionDecl(function: FunctionDecl, payload: CompilingPhaseType): CompilingPhaseType = ???

  override def visitRoot(root: Root, payload: CompilingPhaseType): CompilingPhaseType = ???

  override def visitListExpr(expr: ListExpr, payload: CompilingPhaseType): CompilingPhaseType = ???

  override def visitReturnExpr(expr: ReturnExpr, payload: CompilingPhaseType): CompilingPhaseType = ???

  override def visitTypeExpr(expr: TypeExpr, payload: CompilingPhaseType): CompilingPhaseType = ???

  override def visitCallExpr(expr: CallExpr, payload: CompilingPhaseType): CompilingPhaseType = ???
}

object TypeCheck {
  def apply(): TypeCheck = new TypeCheck()
}

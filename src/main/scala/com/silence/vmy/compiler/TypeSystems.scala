package com.silence.vmy.compiler

import com.silence.vmy.compiler.tree.{AssignmentExpression, BinaryOperateExpression, BlockStatement, CallExpr, FunctionDecl, ListExpr, LiteralExpression, ReturnExpr, Root, TreeVisitor, TypeExpr, Unary, VariableDecl}
import com.silence.vmy.compiler.tree.Tree

sealed class TheType(val name:String) extends CompilingPhaseType

object StringT extends TheType("string")
object DoubleT extends TheType("double")
object IntT extends TheType("int")
object FunT extends TheType("fun")
case class NamedT(override val name: String) extends TheType(name)

class TypeCheck extends TreeVisitor[CompilingPhaseType, CompilingPhaseType]{

  override def visitLiteral(expression: LiteralExpression, payload: CompilingPhaseType): CompilingPhaseType = {
    expression.tag() match {
      case Tree.Tag.DoubleLiteral =>   DoubleT
      case Tree.Tag.StringLiteral =>   StringT
      case Tree.Tag.IntLiteral =>      IntT
      case Tree.Tag.FunctionLiteral => FunT
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

  override def visitVariableDecl(expression: VariableDecl, payload: CompilingPhaseType): CompilingPhaseType = ???

  override def visitAssignment(expression: AssignmentExpression, payload: CompilingPhaseType): CompilingPhaseType = ???

  override def visitFunctionDecl(function: FunctionDecl, payload: CompilingPhaseType): CompilingPhaseType = ???

  override def visitRoot(root: Root, payload: CompilingPhaseType): CompilingPhaseType = ???

  override def visitListExpr(expr: ListExpr, payload: CompilingPhaseType): CompilingPhaseType = ???

  override def visitReturnExpr(expr: ReturnExpr, payload: CompilingPhaseType): CompilingPhaseType = ???

  override def visitTypeExpr(expr: TypeExpr, payload: CompilingPhaseType): CompilingPhaseType = ???

  override def visitCallExpr(expr: CallExpr, payload: CompilingPhaseType): CompilingPhaseType = ???
}


package com.silence.vmy.compiler

import com.silence.vmy.compiler.tree._
import com.silence.vmy.compiler.tree.Tree.Tag
import com.silence.vmy.shared._
import com.silence.vmy.evaluate._
import com.silence.vmy.LCompiler

trait ConstFold extends PerCompileUnitTVisitor {
  import EmulatingValue._
  private val constExpressionEvaluator = new TreeEmulator(new CompileContext(), LCompiler)
  override def leaveUnary(expression: Unary, t : ContextType): Tree = {
    unfoldUnary(expression, t)
  }

  private def unfoldUnary(expression: Unary, t : ContextType): Tree = {
    expression.body() match {
      case body: Unary =>
        (expression.tag, body.tag) match {
          case (Tag.Add, _)  => unfoldUnary(body, t)
          case (Tag.Sub, Tag.Add) => 
            new Unary(Tag.Sub, body.body).setPos(expression.position)
          case (Tag.Sub, Tag.Sub) => body.body
          case _ => expression
        }
      case body: LiteralExpression => {
        if(expression.tag == Tag.Add) body
        else expression
      } 
      case _ => expression
    }
  }

  private def toStringAndKind(v: EmulatingValue): (String, LiteralExpression.Kind) = 
  {
    import LiteralExpression.Kind
    var kind = v match {
      case e: EVInt => Kind.Int
      case e: EVDouble => Kind.Double
      case e: EVLong => Kind.Int
      case e: EVString => Kind.String 
      case e: EVBool => Kind.Boolean
      case _ => null
    }
    (v.toString, kind)
  }
  override def leaveBinary(exp: BinaryOperateExpression, t: ContextType) = 
  { 
    try {
      val evaluatedValue = exp.accept(constExpressionEvaluator, null)
      val (literal, kind) = toStringAndKind(evaluatedValue)
      LiteralExpression.ofStringify(literal ,kind)
    }catch {
      case _ => exp
    }
  }
}

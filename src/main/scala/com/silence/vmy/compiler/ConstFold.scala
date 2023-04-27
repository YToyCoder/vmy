package com.silence.vmy.compiler

import com.silence.vmy.compiler.tree._
import com.silence.vmy.compiler.tree.Tree.Tag
import com.silence.vmy.shared._
import com.silence.vmy.evaluate._

class ConstFold extends TVisitor[Int] {
  import EmulatingValue._
  private val constExpressionEvaluator = new TreeEmulator()
  override def leaveUnary(expression: Unary, t : Int): Tree = {
    unfoldUnary(expression, t)
  }

  private def unfoldUnary(expression: Unary, t : Int): Tree = {
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
  override def leaveBinary(
    expression: BinaryOperateExpression , 
    t: Int) = 
  { 
    try {
      val evaluatedValue = expression.accept(constExpressionEvaluator, null)
      val (literal, kind) = toStringAndKind(evaluatedValue)
      LiteralExpression.ofStringify(literal ,kind)
    }catch {
      case _ => expression
    }
  }
}

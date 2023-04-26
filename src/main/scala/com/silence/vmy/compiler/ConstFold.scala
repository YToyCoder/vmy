package com.silence.vmy.compiler

import com.silence.vmy.compiler.tree._
import com.silence.vmy.compiler.tree.Tree.Tag

trait FoldLiteralValue {
  type LiteralValueType = Int | Long | Double
  def value: LiteralValueType
}
object FoldLiteralValue {
  case class IntValue(value: Int) extends FoldLiteralValue
}
class ConstFold extends TVisitor[Int] {
  import FoldLiteralValue._
  override def leaveUnary(expression: Unary, t : Int): Tree = {
    IntValue(1)
    unfoldUnary(expression, t)
  }

  private def unfoldUnary(expression: Unary, t : Int): Tree = {
    expression.body() match {
      case body: Unary =>
        (expression.tag, body.tag) match {
          case (Tag.Add, _)  => unfoldUnary(body, t)
          case (Tag.Sub, Tag.Add) => new Unary(Tag.Sub, body.body).setPos(expression.position)
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

}

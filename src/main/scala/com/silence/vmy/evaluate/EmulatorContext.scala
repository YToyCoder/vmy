package com.silence.vmy.evaluate

import com.silence.vmy.shared._
import com.silence.vmy.compiler.Context
import com.silence.vmy.evaluate.TreeEmulator.Frame

class FnFrame(pre: Frame) extends Frame(pre) {}

class EmulatorContext extends Context
{
  private var TopFrame: Frame = _
  type VariableType = EmulatingValue.valueType

  def declareVariable(name: String, initValue: VariableType, mutable: Boolean) = 
  {
    val vv /* variable and value */= EmulatingValue(initValue, name, mutable)
    vv.updateScope(TopFrame.TopScope)
    TopFrame.update(name, vv)
    vv
  }

  def enterFrame(): Frame = 
  {
    TopFrame = Frame(TopFrame)
    TopFrame
  }

  def leaveFrame(): this.type =
  {
    TopFrame match 
    {
      case null | Frame(null) => // do nothing
      case Frame(preOne) => TopFrame = preOne
    }
    this
  }

  def enterScope() = TopFrame.enterScope()
  def leaveScope() = TopFrame.leaveScope()

  def lookupVariable(name: String): Option[EmulatingValue] = 
    TopFrame match {
      case null => None
      case frame => frame.lookup(name)
    }
}

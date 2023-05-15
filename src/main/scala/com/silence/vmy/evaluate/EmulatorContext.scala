package com.silence.vmy.evaluate

import com.silence.vmy.shared._
import com.silence.vmy.compiler.Context
import com.silence.vmy.evaluate.TreeEmulator.Frame
import com.silence.vmy.compiler.UpValue
import com.silence.vmy.compiler.CompiledFn
import com.silence.vmy.evaluate.TreeEmulator.ExportValue

class FnFrame(pre: Frame, fn: CompiledFn) extends Frame(pre) 
{
  override def fnBody: Option[CompiledFn] = Some(fn)
}

class RootFrame(pre: Frame) extends Frame(pre)
{
  override def wrapAsExport(name: String): Option[ExportValue] = {
    lookupInScope(name).map(ExportValue(_, name))
  }
}

object RootFrame{
  def unapply(frame: Frame): Option[Frame] = 
    if(frame.isInstanceOf[RootFrame]) Some(frame.pre)
    else None
}

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

  def enterRootFrame() = 
    TopFrame match
      case null => TopFrame = RootFrame(null)
      case _ => throw new RuntimeException("root frame should not have pre frame")
  def leaveRootFrame() = 
    TopFrame match 
      case null => ()
      case RootFrame(pre) => TopFrame = pre
      case _ => throw new RuntimeException("root frame not match type RootFrame")

  def enterFrame(fn: CompiledFn): Frame = 
  {
    TopFrame = FnFrame(TopFrame, fn)
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

  def lookupVariableAsUpValue(name: String): Option[UpValue] = 
    TopFrame match {
      case null => None
      case frame => frame.wrapAsUpValue(name)
    }

  def enterScope() = TopFrame.enterScope()
  def leaveScope() = TopFrame.leaveScope()

  def lookupVariable(name: String): Option[EmulatingValue] = 
    TopFrame match {
      case null => None
      case frame => 
        frame.fnBody match
          case Some(fn) if fn.upvalues != null => 
            fn.upvalues.find(name) 
              match 
                case s @ Some(_) => s
                case _ => frame.lookup(name)
          case _ => 
            frame.lookup(name)
    }
}

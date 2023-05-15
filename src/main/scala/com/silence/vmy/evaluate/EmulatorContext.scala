package com.silence.vmy.evaluate

import com.silence.vmy.shared._
import com.silence.vmy.compiler.Context
import com.silence.vmy.evaluate.TreeEmulator.Frame
import com.silence.vmy.compiler.UpValue
import com.silence.vmy.compiler.CompiledFn
import com.silence.vmy.evaluate.TreeEmulator.ExportValue

import scala.collection.mutable

class FnFrame(pre: Frame, fn: CompiledFn) extends Frame(pre) 
{
  override def fnBody: Option[CompiledFn] = Some(fn)
}

class RootFrame(pre: Frame) extends Frame(pre)
{
  val module: VmyModule = new VmyModule
  override def wrapAsExport(name: String, as : String): Option[ExportValue] = {
    lookupInScope(name) match
      case None => None
      case Some(value) => 
        value match
          case root : RootFrame => 
            val ex = ExportValue(root, name)
            module.register_export(ex, as)
            Some(ex)
          case _ => None
  }
}

object RootFrame{
  def unapply(frame: Frame): Option[Frame] = 
    if(frame.isInstanceOf[RootFrame]) Some(frame.pre)
    else None
}

class VmyModule(val name: String = "") {
  private val _ex_s: mutable.Map[String, ExportValue] = mutable.Map()

  def register_export(ex: ExportValue , as: String): Boolean = {
    if(_ex_s.contains(as)) false
    else 
      _ex_s.addOne((as, ex))
      true
  }
  def vmy_export(name: String): Option[ExportValue] = 
    _ex_s.get(name)
}

class EmulatorContext extends Context
{
  private var TopFrame: Frame = _
  type VariableType = EmulatingValue.valueType

  def declareVariable(name: String, initValue: VariableType, mutable: Boolean) = {
    val vv /* variable and value */= EmulatingValue(initValue, name, mutable)
    vv.updateScope(TopFrame.TopScope)
    TopFrame.update(name, vv)
    vv
  }

  def enterRootFrame() = 
    TopFrame match
      case null => TopFrame = RootFrame(null)
      case _ => throw new RuntimeException("root frame should not have pre frame")
  def leaveRootFrame(): Option[VmyModule] = 
    TopFrame match 
      case null => None
      case root @ RootFrame(pre) => 
        val module = root.asInstanceOf[RootFrame].module
        TopFrame = pre
        Some(module)
      case _ => throw new RuntimeException("root frame not match type RootFrame")

  def enterFrame(fn: CompiledFn): Frame = {
    TopFrame = FnFrame(TopFrame, fn)
    TopFrame
  }

  def leaveFrame(): this.type = {
    TopFrame match {
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
  def register_export(name: String, as: String) : Boolean = 
    TopFrame match
      case null => false
      case frame => 
        frame.wrapAsExport(name, as) match
          case None => false
          case Some(value) => true 
}

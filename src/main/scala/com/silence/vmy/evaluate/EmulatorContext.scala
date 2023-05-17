package com.silence.vmy.evaluate

import com.silence.vmy.shared._
import com.silence.vmy.compiler.Context
import com.silence.vmy.evaluate.TreeEmulator.Frame
import com.silence.vmy.compiler.UpValue
import com.silence.vmy.compiler.CompiledFn
import com.silence.vmy.evaluate.TreeEmulator.ExportValue
import com.silence.vmy.shared.EmulatingValue.EVEmpty
import com.silence.vmy.shared.EmulatingValue.EVObj

import scala.collection.mutable
import java.{util as ju}
import com.silence.vmy.evaluate.TreeEmulator.ScopeNamedValue
import java.io.File

class FnFrame(pre: Frame, fn: CompiledFn) extends Frame(pre) 
{
  override def fnBody: Option[CompiledFn] = Some(fn)
}

class RootFrame(pre: Frame, _f: String) extends Frame(pre)
{
  val module: VmyModule = new VmyModule(this, _f)
  private val _ix : mutable.Map[String, ImportValue] = mutable.Map() 
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

  override def putImportValue(ix: ImportValue, as: String): Boolean = 
    // println(s"is putting $as")
    if(_ix.contains(as)) 
      // println(s"putting $as failed")
      false
    else 
      // println(s"putting $as success")
      _ix.addOne((as, ix))
      true
  
  override def lookup(name: String): Option[EmulatingValue] = 
    // println(s"look variable $name in Root Frame")
    _ix.get(name) match
      case None => 
        // println(s"$name not in import")
        super.lookup(name)
      case Some(value) => 
        // println(s"$name in import")
        value()

  // override to lookup import value
  // override def lookup(name: String): Option[EmulatingValue] = 
}

object RootFrame{
  def unapply(frame: Frame): Option[Frame] = 
    if(frame.isInstanceOf[RootFrame]) Some(frame.pre)
    else None
}

class ImportValue(private val in_scope: Scope, private val _n: String, private val as: String) 
extends ScopeNamedValue(in_scope, _n){
  type ElemType = ImportValue
  override def name(): String = as
}

object VmyModule {
  val ExportAll = "#export#all"
}

class VmyModule(_s: Scope, val name: String = "") {
  private val _ex_s: ExportValue = new ExportValue(_s, "")

  def register_export(ex: ExportValue , as: String): Boolean = {
    _ex_s.put(ex, as)
  }
  def vmy_export(name: String): Option[ExportValue] = 
    if(name == VmyModule.ExportAll) Some(_ex_s)
    else _ex_s.get(name)
  override def toString(): String = s"name($name) => ${_ex_s.toString()}"
}

class EmulatorContext extends Context
{
  private var TopFrame: Frame = _
  type VariableType = EmulatingValue.valueType
  private var c_file: File = null

  def declareVariable(name: String, initValue: VariableType, mutable: Boolean) = {
    val vv /* variable and value */= EmulatingValue(initValue, name, mutable)
    vv.updateScope(TopFrame.TopScope)
    TopFrame.update(name, vv)
    vv
  }

  def current_file() = c_file

  def enterRootFrame(_f: String) = 
    c_file = File(_f)
    TopFrame = new RootFrame(TopFrame, _f)
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
    println(s"try register name:$name as:$as")
    TopFrame match
      case null => false
      case frame => 
        println(s"current frame is ${frame.getClass().getName()}")
        println(s"frame vars ${frame.toString()}")
        frame.wrapAsExport(name, as) match
          case None => false
          case Some(value) => 
            println(s"register name:$name as:$as")
            true 
  
  def register_import(as: String, _s: ImportValue): Boolean = 
    TopFrame match
      case frame @ RootFrame(_) => 
        frame.putImportValue(_s, as)
      case _ => false
}

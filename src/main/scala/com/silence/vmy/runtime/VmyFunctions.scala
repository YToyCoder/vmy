package com.silence.vmy.runtime

import com.silence.vmy.evaluate.EmulatingValue
import scala.collection.mutable
object VmyFunctions{
  private val nativeFunctions = mutable.Map[String, VmyFunction]()
  def lookupFn(name: String): Option[VmyFunction] = nativeFunctions.get(name)

  def register(name: String, fn: VmyFunction) : Unit = 
    nativeFunctions(name) = fn
  def runNative(fn: VmyFunction, params: Array[EmulatingValue]) : EmulatingValue = {
    fn(params)
  }
  trait VmyFunction extends (Array[EmulatingValue] => EmulatingValue)

  register(
    "print", 
    params => {
      print(params.map(_.toString).reduce(_ + _))
      EmulatingValue.EVEmpty
    })
  register(
    "println", 
    params => {
      println(params.map(_.toString).reduce(_ + _))
      EmulatingValue.EVEmpty
    })
}




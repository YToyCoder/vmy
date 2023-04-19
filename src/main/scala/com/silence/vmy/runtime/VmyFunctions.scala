package com.silence.vmy.runtime

import com.silence.vmy.evaluate.EmulatingValue
import com.silence.vmy.evaluate.EmulatingValue._

import scala.collection.mutable
import java.util.List

object VmyFunctions{
  private val nativeFunctions = mutable.Map[String, VmyFunction]()
  def lookupFn(name: String): Option[VmyFunction] = nativeFunctions.get(name)

  def register(name: String, fn: VmyFunction) : Unit = 
    nativeFunctions(name) = fn
  def runNative(fn: VmyFunction, params: List[EmulatingValue]) : EmulatingValue = {
    fn(params)
  }
  trait VmyFunction extends (List[EmulatingValue] => EmulatingValue)

  register(
    "print", 
    params => {
      print(params.stream.map(_.toString).reduce(_ + _).get)
      EmulatingValue.EVEmpty
    })
  register(
    "println", 
    params => {
      println(params.stream.map(_.toString).reduce(_ + _).get)
      EmulatingValue.EVEmpty
    })

  val ListElementGetter: String = "#arr#get"
  val ListElementUpdate: String = "#arr#update"
  register(
    ListElementGetter,
    params => {
      params.size() match {
        case 0 | 1 => 
          throw new VmyRuntimeException(s"${ListElementGetter} need at least 2 parameters")
        case otherwise => {
          (params.get(0), params.get(1)) match {
            case (EVList(arr),EVInt(index)) => arr.get(index)
            case _ => 
              throw new VmyRuntimeException(s"${ListElementGetter} need parameters (arr, int)")
          }
        } 
      }
    }
  )
}




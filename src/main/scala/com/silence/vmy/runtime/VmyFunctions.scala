package com.silence.vmy.runtime

import com.silence.vmy.evaluate.EmulatingValue
import com.silence.vmy.evaluate.EmulatingValue._

import scala.collection.mutable
import java.util.List
import java.util.ArrayList
import java.util.stream.IntStream

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

  // range function:
  // for a, i in range(3,4) {
  //
  // }
  register(
    "range", 
    params => {
      params.size() match {
        case 1 => 
          params.get(0) match{
            case EVInt(end) => EmulatingValue(rangeI(0, end))
            case _ => throw new VmyRuntimeException(s"range parameter should be integer")
          }
        case 2 => 
          (params.get(0), params.get(1)) match {
            case (EVInt(start), EVInt(end)) => EmulatingValue(rangeI(start, end))
            case _ => throw new VmyRuntimeException(s"range parameter should be integer")
          }
        case _ => throw new VmyRuntimeException(s"not support this call for range")
      }
    })

  private def rangeI(start: Int, end: Int): List[EmulatingValue] = new ArrayList(IntStream.range(start, end).mapToObj(EmulatingValue(_)).toList)

  register(
    ListElementUpdate,
    params => {
      params.size() match {
        case 3 => 
          (params.get(0), params.get(1), params.get(2)) match {
            case (EVList(arr), EVInt(index), value) => arr.set(index, value)
            case _ => 
              throw new VmyRuntimeException(s"${ListElementGetter} need parameters (arr, int, ?)")
          }
        case _ => 
          throw new VmyRuntimeException(s"${ListElementGetter} need exactly 3 parameters")
      }
    }
  )
}




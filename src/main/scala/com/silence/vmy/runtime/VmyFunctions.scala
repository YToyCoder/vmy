package com.silence.vmy.runtime

import com.silence.vmy.shared.EmulatingValue
import com.silence.vmy.shared.EmulatingValue._

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
  def tryRun(name: String, params: List[EmulatingValue]): EmulatingValue = 
  {
    lookupFn(name) match 
    {
      case Some(fn) => fn(params)
      case None => throw new VmyRuntimeException(s"not found fn ${name}")
    }
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

  val ElementGetter: String = "#all#get"
  val ElementUpdate: String = "#all#update"
  val ArrayAppend : String = "#arr#append"
  register(
    ElementGetter,
    params => {
      params.size() match {
        case 0 | 1 => 
          throw new VmyRuntimeException(s"${ElementGetter} need at least 2 parameters")
        case otherwise => {
          (params.get(0), params.get(1)) match {
            case (EVList(arr),EVInt(index)) => arr.get(index)
            case (EVObj(obj), EVString(name)) => obj.get(name)
            case _ => 
              throw new VmyRuntimeException(s"${ElementGetter} need parameters (arr, int)")
          }
        } 
      }
    }
  )

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
    ElementUpdate,
    params => {
      params.size() match {
        case 3 => 
          (params.get(0), params.get(1), params.get(2)) match {
            case (ret @ EVList(arr), EVInt(index), value) => { 
              arr.set(index, value)
              ret
            }
            case (ret @ EVObj(obj),  EVString(name), value) => { 
              obj.put(name, value) 
              ret
            }
            case _ => 
              throw new VmyRuntimeException(s"${ElementUpdate} need parameters (arr, int, ?)")
          }
        case _ => 
          throw new VmyRuntimeException(s"${ElementUpdate} need exactly 3 parameters")
      }
    }
  )

  register(
    ArrayAppend,
    params => {
      params.size() match 
      {
        case 2 => 
          (params.get(0), params.get(1)) match {
            case (one @ EVList(arr), value) => {
              arr.add(value)
              one
            }
          }
        case _ => 
          throw new VmyRuntimeException(s"array append method in wrong parameters ${params.toString}") 
      }
    }
  )

}




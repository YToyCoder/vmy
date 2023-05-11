package com.silence.vmy.compiler

import com.silence.vmy.evaluate.EmulatorContext
import com.silence.vmy.compiler.tree._
import com.silence.vmy.compiler.UpValue
import com.silence.vmy.compiler.UpValues

import scala.collection.mutable

case class VariableAndType(val name: String, val tId: String) {}

// todo scope
class VariableDeclarationAndUpValuesChecking extends PerCompileUnitTVisitor
{
  private val variableAndType: mutable.Map[String, VariableAndType] = mutable.Map()
  private val upvalues : mutable.Map[String,UpValue] = mutable.Map()
  protected def cleanVariable(): Unit = variableAndType.clear()

  override def leaveIdExpr(exp: IdExpr, context: CompileContext): Tree = 
  {
    val id = exp.name()
    variableAndType.get(id) match
    {
      case None => 
        tryFindUpValueAndCache(id, context) 
        // match
        exp
        //   case None => throw new RuntimeException(s"variable not exist ${id}")
        //   case Some(_) => exp

      case Some(_) => exp
    }
  }

  private def tryFindUpValueAndCache(name: String, context: EmulatorContext): Option[UpValue] =
  {
    upvalues.get(name) match {
      case None =>
        context.lookupVariableAsUpValue(name) match {
          case None => None
          case up @ Some(upvalue) =>
            upvalues.addOne((name, upvalue))
            up
        }
      case up @ Some(_) => up
    }
  }

  override def leaveFunctionDecl(fndecl: FunctionDecl, context: CompileContext): Tree =
  {
    // todo 
    fndecl
  }

  private def typeExpToString(texp: TypeExpr): String =
  {
    texp match
      case null => ""
      case _    => texp.typeId()
  }

  override def leaveVariableDecl(exp: VariableDecl, context: CompileContext) : Tree =
  { 
    val variableName = exp.name()
    if(variableAndType.contains(variableName)) 
      throw new RuntimeException(s"redeclare variable ${variableName}")
    else 
      variableAndType.addOne((variableName, VariableAndType(variableName, typeExpToString(exp.t()))))
      exp
  }

  def getUpvalues() : UpValues =
  {
    UpValues {
      upvalues
      .map((name, upvalue) => upvalue)
      .toArray
    } 
  }
}

package com.silence.vmy.compiler

import com.silence.vmy.evaluate.EmulatorContext
import com.silence.vmy.compiler.tree._
import com.silence.vmy.compiler.UpValue
import com.silence.vmy.compiler.UpValues

import scala.collection.mutable
import com.silence.vmy.runtime.VmyFunctions
import java.{util => ju}

// todo scope
class VariableDeclarationAndUpValuesChecking extends PerCompileUnitTVisitor
{
  private var topScope = Scope(null)
  private val upvalues : mutable.Map[String,UpValue] = mutable.Map()
  protected def cleanVariable(): Unit = topScope = Scope(null)
  private def enterScope() = topScope = Scope(topScope)
  private def leaveScope() = 
  {
    topScope match 
      case null => 
      case Scope(pre) => 
        topScope = pre
  }

  case class Scope(pre: Scope) 
  {
    private val vars: mutable.Set[String] = mutable.Set()
    def declareVar(name: String): Boolean = 
      if (contains(name)) then false
      else 
        vars.add(name)
        true
    def contains(name: String): Boolean = 
    {
      if(vars.contains(name)) then true
      else 
        pre match
          case null => false
          case  _  => pre.contains(name)
    }
  }

  private def localExists(name:String): Boolean = 
  {
    val localVarExists = 
    topScope match 
      case null => false
      case _ => topScope.contains(name)
    if(localVarExists) true
    else 
      VmyFunctions.lookupFn(name) match
        case None => false
        case Some(value) =>  true
  }

  private def declare(name: String) = 
    topScope match 
      case null => false
      case _ => topScope.declareVar(name)

  override def leaveIdExpr(exp: IdExpr, context: CompileContext): Tree = 
  {
    val id = exp.name()
    if(!localExists(id)) 
      tryFindUpValueAndCache(id, context) 
    exp
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
    if( null != fndecl)
      declare(fndecl.name())
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
    if(declare(variableName)) exp
    else throw new RuntimeException(s"redeclare variable ${variableName}")
  }

  def getUpvalues() : UpValues =
  {
    UpValues {
      upvalues
      .map((name, upvalue) => upvalue)
      .toArray
    } 
  }
  private def handleCondition(state: ConditionStatement, context: CompileContext) = 
  {
    state match 
      case null => 
      case _ => 
        state.condition().accept(this, context)
        enterScope()
        state.block().accept(this, context)
        leaveScope()
  }

  override def enterIfStatement(ifStatement: IfStatement, context: CompileContext): Boolean = 
  {
    ifStatement match
      case null => 
      case _ =>
        handleCondition(ifStatement.ifStatement(), context)
        ifStatement.elif() match
          case null => 
          case elif => elif.forEach(handleCondition(_, context))
        ifStatement.el() match
          case null => 
          case block => 
            enterScope()
            block.accept(this, context)
            leaveScope()

    false
  }

  override def enterForStatement(state: ForStatement, context: CompileContext): Boolean =
  {
    enterScope()
    if(state.heads() != null){
      state.heads().forEach{ id =>
        declare(id.name())
      }
    }
    if(state.arrId() != null){
      state.arrId().accept(this, context)
    }
    true
  }

  override def leaveForStatement(state: ForStatement, context: CompileContext): Tree = 
  {
    leaveScope()
    state
  }

  // todo:
  override def leaveCallExpr(call: CallExpr, context: CompileContext): Tree = 
  {
    if(!localExists(call.callId())) 
      tryFindUpValueAndCache(call.callId(), context)
    call
  }
}

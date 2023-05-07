package com.silence.vmy.evaluate

import com.silence.vmy.compiler.tree.Tree.Tag
import com.silence.vmy.compiler.tree._
import com.silence.vmy.tools.Log
import com.silence.vmy.compiler.{Modifiers, Tokens}
import com.silence.vmy.shared.EmulatingValue.{RetValue, initValue}
import com.silence.vmy.runtime.VmyRuntimeException
import com.silence.vmy.runtime.VmyFunctions
import com.silence.vmy.shared.EmulatingValue.EVEmpty.mkOrderingOps
import com.silence.vmy.shared._

import math.Fractional.Implicits.infixFractionalOps
import math.Integral.Implicits.infixIntegralOps
import math.Numeric.Implicits.infixNumericOps

import java.util.List
import java.util.ArrayList
import java.util.HashMap
import java.util.Map

import scala.annotation.tailrec
import scala.collection.mutable

object TreeEmulator {
  case class Frame(pre: Frame) extends Scope(pre) 
  {
    var TopScope: Scope = this
    def enterScope() = 
    {
      TopScope = Scope(TopScope)
    }

    def leaveScope(): Unit = 
      if (TopScope != this)
      {
        TopScope = TopScope.preOne
      }
  }
}

class TreeEmulator(private val context: EmulatorContext) 
  extends Log 
  with TreeVisitor[EmulatingValue, EmulatingValue]  {

  import EmulatingValue.{EVEmpty, EVFunction, EVList, EVObj, Zero}
  var debug: Boolean = false
  private def createFrame() : TreeEmulator.Frame = context.enterFrame()
  private def exitFrame() : Unit = context.leaveFrame()
  private def createScope() = context.enterScope()
  private def exitScope() = context.leaveScope()
  private def lookupVariable(id: String) = context.lookupVariable(id)

  private def declareVariable(name: String, initValue: EmulatingValue.valueType, mutable: Boolean): EmulatingValue = 
    context.declareVariable(name, initValue, mutable)

  private def declareVariable(name: String, initValue: EmulatingValue.valueType): EmulatingValue =
    declareVariable(name, initValue, true)

  override def visitLiteral(expression: LiteralExpression, payload: EmulatingValue): EmulatingValue = {
    val literal = expression.literal().asInstanceOf[String]
    if(expression.isInt) EmulatingValue(literal.toInt)
    else if(expression.isBoolean) EmulatingValue(literal match {
      case "true" => true
      case "false" => false
      case _ => throw new Exception()
    })
    else if(expression.isDouble) EmulatingValue(literal.toDouble)
    else if(expression.isString) EmulatingValue(literal)
    else throw new Exception("error in visiting literal")
  }

  override def visitUnary(expression: Unary, payload: EmulatingValue): EmulatingValue =
    expression.tag() match {
      case Tag.Add => expression.body().accept(this, payload)
      case Tag.Sub => EmulatingValue.reverse(expression.body().accept(this, payload))
      case _ => throw new Exception(s"error${Tokens.stringifyLocation(expression.position())}")
    }

  override def visitBlock(statement: BlockStatement, payload: EmulatingValue): EmulatingValue = {
    val expressions = statement.exprs()
    @tailrec
    def visitEach(i : Int): EmulatingValue = {
      if(i >= expressions.size()) EVEmpty 
      else {
        val el = expressions.get(i)
        val v = el.accept(this, payload)
        if(debug) {
          log(">#"*10)
          log(s"block-elemtent => ${el} \n|>> is RetValue => ${v.isInstanceOf[RetValue]}")
          log("<#"*10)
        }
        if(v.isInstanceOf[RetValue]) v
        else visitEach(i + 1)
      }
    }
    visitEach(0)
  }

  override def visitBinary(expression: BinaryOperateExpression, payload: EmulatingValue): EmulatingValue = {
    val left = expression.left().accept(this, payload)
    val right= expression.right().accept(this, payload)
    expression.tag() match {
      case Tag.Add | Tag.Concat => left + right
      case Tag.Sub => left - right
      case Tag.Multi => left * right
      case Tag.Div => left / right
      case Tag.AddEqual => {
        val temp = left + right
        left.update(temp)
        temp
      }
      case Tag.SubEqual => {
        val temp = left - right
        left.update(temp)
        temp
      }
      case Tag.MultiEqual => {
        val temp = left * right
        left.update(temp)
        temp
      }
      case Tag.DivEqual => {
        val temp = left / right
        left.update(temp)
        temp
      }
      case Tag.Equal => EmulatingValue(left == right)
      case Tag.NotEqual => EmulatingValue(left != right)
      case Tag.Less => EmulatingValue((left - right) < Zero)  
      case Tag.Greater => EmulatingValue((left - right) > Zero )
      case Tag.Le => EmulatingValue((left - right) <= Zero )
      case op => throw new Exception(s"binary error/ ${Tokens.stringifyLocation(expression.position())} ${op}")
    }
  }

  override def visitVariableDecl(expression: VariableDecl, payload: EmulatingValue): EmulatingValue = {
    val name = expression.name()
    val variable_type = expression.t()
    val initv = payload match {
      case null => 
        if(variable_type == null) "" 
        else EmulatingValue.initValue(variable_type.typeId())
      case otherwise => payload.value
    }
    declareVariable(
      name,
      initv,
      !expression.modifiers().is(Modifiers.CVariableConst))
  }

  override def visitAssignment(expression: AssignmentExpression, payload: EmulatingValue): EmulatingValue = {
    val value = expression.right().accept(this, payload)
    val left = expression.left
    val destination = left.accept(this, value)
    if(debug) {
      log(s"assignment: ${destination.name}(variable-name) <= ${value} ")
    }
    if(!left.isInstanceOf[VariableDecl]) {
      return destination() = value
    }
    return destination
  }

  override def visitFunctionDecl(function: FunctionDecl, payload: EmulatingValue): EmulatingValue = {
    val name = function.name()
    declareVariable(name, function)
  }

  override def visitRoot(root: Root, payload: EmulatingValue): EmulatingValue = {
    createFrame()
    val returnValue = root.body().accept(this, payload)
    exitFrame()
    returnValue
  }

  override def visitListExpr[E <: Expression](expr: ListExpr[E], payload: EmulatingValue): EmulatingValue = {
    expr.body().forEach(el => el.accept(this, payload))
    EVEmpty
  }

  override def visitReturnExpr(expr: ReturnExpr, payload: EmulatingValue): EmulatingValue = 
    RetValue(expr.body().accept(this, payload))

  override def visitTypeExpr(expr: TypeExpr, payload: EmulatingValue): EmulatingValue = null

  override def visitCallExpr(call: CallExpr, payload: EmulatingValue): EmulatingValue = {
    def paramsEval() = call.params.body.stream()
      .map(_.accept(this, null))
      .toList()
    def callJavaNative(fnName: String, params: List[EmulatingValue]): EmulatingValue = {
       VmyFunctions.lookupFn(fnName) match {
        case Some(fn) => VmyFunctions.runNative(fn, params)
        case None => 
          throw new VmyRuntimeException(s"not exists function : ${call.callId()}")
      }
    }
    def listConcat(a: List[EmulatingValue], b : List[EmulatingValue]) = {
      a.addAll(b)
      a
    }
    if(debug) log(s"visiting call ${call.callId()}")
    lookupVariable(call.callId()) match {
      // function in frame
      case Some(fnTreeKeeper) if (fnTreeKeeper.isInstanceOf[EVFunction]) => {
        if(debug) { log(s"Fn : ${call.callId} is user defined") }
        createFrame()
        val fnTree = fnTreeKeeper.asInstanceOf[EVFunction].value
        val paramsValues = paramsEval()
        // declare variable with initvalue
        for(i <- 0 until fnTree.params.size){
          if(i < paramsValues.size){
            fnTree.params.get(i).accept(this, paramsValues.get(i)) 
          }else fnTree.params.get(i).accept(this, EVEmpty)
        }
        // eval body
        val result = fnTreeKeeper.asInstanceOf[EVFunction].value.body.accept(this, null)
        exitFrame()
        result match {
          case RetValue(value) => value
          case _ => result
        }
      } 
      /* =>>>two function call
       * list and obj have two function : 
       *  1. element get
       *  2. update
       **/
      case Some(value) if value.isInstanceOf[EVList] || value.isInstanceOf[EVObj] => {
        if(debug) log(s"Fn : ${call.callId} is arr method")
        call.params.body.size() match {
          case 1 => 
            callJavaNative(
              VmyFunctions.ElementGetter, 
              listConcat(new ArrayList(List.of(value)),paramsEval()))
          case 2 => 
            callJavaNative(
              VmyFunctions.ElementUpdate,
              listConcat(new ArrayList(List.of(value)), paramsEval()))
          case _ => throw new VmyRuntimeException(s"EVList have no ${call.callId} method")
        }
      }
      // function defined in java
      case _ => {
        if(debug) log(s"Fn : ${call.callId} is java native method")
        callJavaNative(call.callId(), paramsEval())
      } 
    }
  }

  override def visitIdExpr(expr: IdExpr, payload: EmulatingValue): EmulatingValue = {
    val idName = expr.name()
    lookupVariable(idName) match {
      case None => throw new VmyRuntimeException(s"not exists ${idName}");
      case Some(value) => value
    }
  }

  override def visitIfStatement(statement: IfStatement, payload: EmulatingValue): EmulatingValue = {
    val ifStatement = statement.ifStatement()
    ifStatement.condition().accept(this, payload) match {
      case e if e != EVEmpty && e.toBool => 
        createScope()
        val v = ifStatement.block().accept(this, payload)
        exitScope()
        v
      case _ => {
        val (elifResult, content) = doWithElif(statement, payload)
        if(elifResult) 
          return content
        return statement.el() match {
          case null => EmulatingValue.EVEmpty
          case block => block.accept(this, payload)
        }
      }
    }
  }

  override def visitForStatement(statement: ForStatement, payload: EmulatingValue): EmulatingValue = {
    if(debug) {
      log("enter ForStatement")
    } 
  // create new frame for for-statement
    val heads = statement.heads
    // heads.stream().forEach(declareVariableForId _)
    val result: EmulatingValue = statement.arrId.accept(this, payload) match {
      case EVList(value) => {
        // declare all variables : element id  and index id 
        for(index <- 0 until value.size){
          createScope() 
          val indexExpr = LiteralExpression.ofStringify(index.toString, LiteralExpression.Kind.Int)
          declareVariable(heads.get(0).name, value.get(index), false)
          if(statement.isWithIndex()){
            // assign index
            // val indexAssignExpression = createAssignment(heads.get(1), indexExpr)
            if(debug){
              log(s"set index variale ")
            }
            // eval index assignment
            declareVariable(heads.get(1).name, indexExpr.accept(this, payload))
          }
          statement.body.accept(this, payload) match{
            case e : RetValue => {
              return e
            }
            case _ => 
          }
          exitScope()
        }
        EVEmpty
      } 
      case e => throw new VmyRuntimeException(s"${e.name} not support for iterate")
    }
    result
  }

  private[this] def doWithElif(statement: IfStatement, payload: EmulatingValue): (Boolean, EmulatingValue) = {
    val elifs = statement.elif()
    val length = elifs.size()
    @tailrec // replace for loop as tailrec function
    def handleUntilSatisfied(loc: Int): (Boolean, EmulatingValue) = 
    {
      val current = elifs.get(loc)
      if(loc >= length) (false, EVEmpty)
      else if(current.condition().accept(this, payload).toBool)
      {
        createScope()
        val v = (true, current.block().accept(this, payload))
        exitScope()
        v
      }
      else handleUntilSatisfied(loc + 1) 
    }
    handleUntilSatisfied(0)
  }

  override def visitArr(arr: ArrExpression, payload: EmulatingValue) : EmulatingValue =
    EmulatingValue{ 
      copyList{ 
        arr.elements
          .stream()
          .map(_.accept(this, payload))
          .toList
      }
    }
  private def copyList[T](origin: List[T]): List[T] = new ArrayList(origin)

  private def copyMap(map: Map[String, EmulatingValue]): Map[String, EmulatingValue] = new HashMap(map)
  override def  visitVmyObject(obj: VmyObject, t: EmulatingValue): EmulatingValue = {
    EmulatingValue{ 
      copyMap {
        obj.properties()
          .entrySet()
          .stream()
          .map( entry => Map.of(entry.getKey(), entry.getValue().accept(this, t)) )
          .reduce(new HashMap[String, EmulatingValue](), (map, each) => { map.putAll(each); map })
      }
    }
  }
}

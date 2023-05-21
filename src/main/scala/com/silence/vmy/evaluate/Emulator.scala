package com.silence.vmy.evaluate

import com.silence.vmy.compiler.tree.Tree.Tag
import com.silence.vmy.compiler.tree._
import com.silence.vmy.compiler.Compiler
import com.silence.vmy.compiler.CompileContext
import com.silence.vmy.tools.Log
import com.silence.vmy.compiler.{Modifiers, Tokens}
import com.silence.vmy.shared.EmulatingValue.{RetValue, initValue}
import com.silence.vmy.runtime.VmyRuntimeException
import com.silence.vmy.runtime.VmyFunctions
import com.silence.vmy.shared.EmulatingValue.EVEmpty.mkOrderingOps
import com.silence.vmy.shared._
import com.silence.vmy.compiler.CompileUnit.wrapAsCompiledFn
import com.silence.vmy.compiler.CompiledFn
import com.silence.vmy.compiler.CompileUnit.wrapAsCompileUnit
import com.silence.vmy.compiler.UpValuePhase
import com.silence.vmy.compiler.Compilers.CompileUnit
import com.silence.vmy.compiler.UpValue

import math.Fractional.Implicits.infixFractionalOps
import math.Integral.Implicits.infixIntegralOps
import math.Numeric.Implicits.infixNumericOps

import java.util.List
import java.util.ArrayList
import java.util.HashMap
import java.util.Map
import java.{util as ju}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.control.NonLocalReturns._
import com.silence.vmy.evaluate.TreeEmulator.ExportValue
import com.silence.vmy.tools.Utils.error
import java.io.File
import com.silence.vmy.shared.EmulatingValue.EVEmpty
import com.silence.vmy.shared.EmulatingValue.EVObj

// interal tree that use to register a module object
class RegisterModuleTree(val position: Long = 0) extends Tree {
  override def accept[R,T](visitor: TreeVisitor[R,T], payload: T): R = 
    visitor match
      case vs : ExtendsVisitor[R,T] => vs.visitRegisterModule(this, payload)
      case _ => null.asInstanceOf[R]
  override def accept[T](visitor: TVisitor[T], t: T): Tree = this
  override def tag(): Tag = null
  override def toString(): String = "Register-Module"
}

trait ExtendsVisitor[R,T] extends TreeVisitor[R,T] {
  def visitRegisterModule(tree: RegisterModuleTree, payload: T): R
}

// 
object TreeEmulator {
  case class Frame(pre: Frame) extends Scope(pre) 
  {
    var TopScope: Scope = this
    def enterScope() = 
      TopScope = Scope(TopScope)

    def leaveScope(): Unit = 
      if (TopScope != this) {
        TopScope = TopScope.preOne
      }

    def wrapAsExport(name: String, as: String): Option[ExportValue] = None 

    def fnBody: Option[CompiledFn] = None
    def putImportValue(ix: ImportValue, as: String): Boolean = false
    def exec_file() : String = 
      pre match
        case null => ""
        case _ => pre.exec_file()
      
  }

  case class ExportValue(in_scope: Scope, _n: String) 
    extends ScopeNamedValue(in_scope, _n){
    type EleType = ExportValue
  }

  class ScopeNamedValue(protected val _in_scope: Scope, private val _n: String) { self =>
    type EleType <: ScopeNamedValue
    protected val map: mutable.Map[String, EleType] = mutable.Map()
    def is_ = _in_scope
    def apply() = 
      if(_n == "") {
        val _map: ju.Map[String, EmulatingValue] = ju.HashMap()
        map.foreach{ (name, s_n) => 
          s_n.is_.lookup(s_n.name()) match
            case None => _map.put(name, EVEmpty)
            case Some(value) => _map.put(name, value)
        }
        Some(EVObj(_map))
      }
      else _in_scope.lookup(_n)
    def name() = _n 
    def put(v: EleType, as :String) = {
      if(map.contains(as)) false
      else 
        map.addOne((as, v))
        true
    }

    def get(name: String): Option[EleType] = map.get(name)

    override def toString(): String = map.mkString
  }
}

object UpValueCompiler extends Compiler[CompileContext] {

  def compile(context: CompileContext, unit: CompileUnit) =
    if(unit.compiled()) unit
    else UpValuePhase.run(context, unit)
}

class TreeEmulator(
  val context: CompileContext, 
  private val compiler: Compiler[CompileContext]
  )
  extends Log 
  with ExtendsVisitor[EmulatingValue, EmulatingValue]  {
  // with TreeVisitor[EmulatingValue, EmulatingValue]  {

  import EmulatingValue.{EVEmpty, EVFunction, EVList, EVObj, Zero}
  var debug: Boolean = false
  private val cached_mdoules : mutable.Map[String, EVObj] = mutable.Map()
  private val loader: Loader = new Loader(this)
  private def createRootFrame(_f: String) = 
    println(s"create frame for $_f")
    context.enterRootFrame(_f)
  private def exitRootFrame() = context.leaveRootFrame()
  private def createFrame(fn: CompiledFn) : TreeEmulator.Frame = context.enterFrame(fn)
  private def exitFrame() : Unit = context.leaveFrame()
  private def createScope() = context.enterScope()
  private def exitScope() = context.leaveScope()
  private def lookupVariable(id: String) = context.lookupVariable(id)
  private def cache_module(name: String,module: EVObj) = 
    if(module != null && !cached_mdoules.contains(name))
      cached_mdoules.addOne((name, module))
      false
    else true

  private def lookup_module(name: String): Option[EVObj] = 
    // println(s"cached modules : $cached_mdoules")
    cached_mdoules.get(name)

  private def lookup_export_in_module(name: String, module: String) : Option[ExportValue] = {
    None
  }

  def run(ast: Tree) = ast.accept(this, EVEmpty)

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
    // declareVariable(name, function)
    val compiledfn = UpValueCompiler.compile(context, wrapAsCompileUnit(function))
    declareVariable(name, wrapAsCompiledFn(compiledfn.node()))
  }

  override def visitRoot(root: Root, payload: EmulatingValue): EmulatingValue = {
    createRootFrame(root.file_name())
    val body = root.body
    val returnValue = root.body().accept(this, payload)
    exitRootFrame()
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
          throw new VmyRuntimeException(s"not exists function : ${call.callId()}: ${context.current_file().getAbsolutePath()} > ${Tokens.stringifyLocation(call.position())}")
      }
    }
    def listConcat(a: List[EmulatingValue], b : List[EmulatingValue]) = {
      a.addAll(b)
      a
    }
    if(debug) log(s"visiting call ${call.callId()}")

    def listOrObjCall(obj: EmulatingValue): EmulatingValue = 
      if(debug) log(s"Fn : ${call.callId} is arr method")
      call.params.body.size() match {
        case 1 => 
          callJavaNative(
            VmyFunctions.ElementGetter, 
            listConcat(new ArrayList(List.of(obj)),paramsEval()))
        case 2 => 
          callJavaNative(
            VmyFunctions.ElementUpdate,
            listConcat(new ArrayList(List.of(obj)), paramsEval()))
        case _ => throw new VmyRuntimeException(s"EVList have no ${call.callId} method")
      }
    lookupVariable(call.callId()) match {
      // function in frame
      case Some(fnTreeKeeper) if (fnTreeKeeper.isInstanceOf[EVFunction]) => {
        if(debug) { log(s"Fn : ${call.callId} is user defined") }
        val fnTree = fnTreeKeeper.asInstanceOf[EVFunction].value
        createFrame(wrapAsCompiledFn(fnTree))
        val paramsValues = paramsEval()
        // declare variable with initvalue
        for(i <- 0 until fnTree.params.size){
          if(i < paramsValues.size){
            // declared in function
            fnTree.params.get(i).accept(this, paramsValues.get(i)) 
          }else // not declared in function
            fnTree.params.get(i).accept(this, EVEmpty)
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
        listOrObjCall(value)
      }
      case Some(value)
        if value.isInstanceOf[UpValue] && EmulatingValue.upvalueIsListOrObj(value.asInstanceOf[UpValue]) =>
        value.asInstanceOf[UpValue].variable_value 
          match
            case Some(upvalue) => listOrObjCall(upvalue)
            case None =>  EVEmpty // should be not reach
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
    unwrapIfIsUpvalue(ifStatement.condition().accept(this, payload)) match {
      case e if e != EVEmpty && e.toBool => {
        createScope()
        val v = ifStatement.block().accept(this, payload)
        exitScope()
        v
      }
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

  private def unwrapIfIsUpvalue(value: EmulatingValue): EmulatingValue =
    value match
      case upvalue @ UpValue(_, _) => 
        upvalue.variable_value match
          case None => EVEmpty
          case Some(value) => value
      case _ => value

  override def visitForStatement(statement: ForStatement, payload: EmulatingValue): EmulatingValue = {
    if(debug) {
      log("enter ForStatement")
    } 
  // create new frame for for-statement
    val heads = statement.heads
    val result: EmulatingValue = returning{ 
      unwrapIfIsUpvalue(statement.arrId.accept(this, payload)) match {
        case EVList(value) => {
          // declare all variables : element id  and index id 
          for(index <- 0 until value.size){
            createScope() 
            val indexExpr = LiteralExpression.ofStringify(index.toString, LiteralExpression.Kind.Int)
            declareVariable(heads.get(0).name, value.get(index), false)
            if(statement.isWithIndex()){
              // assign index
              if(debug){
                log(s"set index variale ")
              }
              // eval index assignment
              declareVariable(heads.get(1).name, indexExpr.accept(this, payload))
            }
            statement.body.accept(this, payload) match{
              case e : RetValue => {
                throwReturn(e) // return e
              }
              case _ => 
            }
            exitScope()
          }
          EVEmpty
        } 
        case e => throw new VmyRuntimeException(s"${e.name} not support for iterate")
      }
    }
    result
  }

  private[this] def doWithElif(statement: IfStatement, payload: EmulatingValue): (Boolean, EmulatingValue) = {
    val elifs = statement.elif()
    val length = elifs.size()
    @tailrec // replace for loop as tailrec function
    def handleUntilSatisfied(loc: Int): (Boolean, EmulatingValue) = 
    {
      val current = if(loc < length) elifs.get(loc) else null
      if(loc >= length) (false, EVEmpty)
      else if(current.condition().accept(this, payload).toBool) {
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

  override def visitExport(state: ExportState, payload: EmulatingValue): EmulatingValue = 
  {
    def rg(ex: ExportState.ExportExp) = {
      if(ex.hasAlias()){
        context.register_export(ex.name(), ex.alias())
      }else {
        context.register_export(ex.name(), ex.name())
      }
    }
    if(state.isOne()) {
      val ex = state.getOne()
      rg(ex)
    }else {
      val ex_s = state.getAll()
      ex_s.forEach(rg)
    }
    EVEmpty
  }

  private def get_current_dir() : String = {
    // println(s"get dir -> cur ${context.current_file().getAbsolutePath()}")
    context.current_file().getAbsoluteFile().getParent()
  }

  override def visitImport(state: ImportState, payload: EmulatingValue): EmulatingValue =
  {
    if(state == null) EVEmpty
    else {
      // todo : find module or else load module
      val uri = s"${get_current_dir()}\\${state.uri()}"
      println(s"current file ${context.current_file().getAbsolutePath()}")
      println(s"get module file => $uri")
      def getModule() = 
        lookup_module(uri) match
          case None => 
            loader.load(uri) 
            lookup_module(uri) match
              case None => None
              case s @ Some(value) => s
          case s @ Some(value) => s
      getModule() match 
        case None => 
        case Some(value) => 
          val ipt = state.oneImport() 
          val ipt_name = if(ipt.hasAlias()) ipt.alias() else ipt.name()
          // println(s"ipt name is => $ipt_name")
          declareVariable(ipt_name, value)
      EVEmpty
    }
  }

  override def visitRegisterModule(tree: RegisterModuleTree, payload: EmulatingValue): EmulatingValue = 
    // do => lookup the variable called __export
    val current_file = context.current_file().getAbsolutePath() 
    println(s"register module $current_file")
    lookupVariable("__export") match
      case Some(value) if value.isInstanceOf[EVObj] => 
        cache_module(current_file, value.asInstanceOf[EVObj])
      case _ => 
        throw new RuntimeException(s"cache current module error, not exists object named __export in current file ${current_file}")
    EVEmpty
}

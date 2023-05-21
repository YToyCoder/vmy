package com.silence.vmy.evaluate

import com.silence.vmy.compiler.tree._
import com.silence.vmy.compiler.Compilers.CompileUnit
import com.silence.vmy.compiler.GeneralScanner
import com.silence.vmy.compiler.GeneralParser
import com.silence.vmy.tools.Log

import java.io.FileNotFoundException
import com.silence.vmy.tools.Utils.error
import com.silence.vmy.compiler.CompileUnit.wrapAsCompileUnit
import com.silence.vmy.compiler.LCompiler
import com.silence.vmy.tools.TreePrinter

class Loader(emulator: TreeEmulator) extends Log {
  private val printer = new TreePrinter()
  def load(uri: String): Option[VModule] = {
    doParsing(uri) match
      case None => None
      case Some(value) => 
        val compiledUnit = LCompiler.compile(emulator.context, value)
        // println(printer.tree_as_string(compiledUnit.node()))
        emulator.run(compiledUnit.node()) match
          case null => None
          case _ => None
        
  }

  private def doParsing(uri: String): Option[CompileUnit] = {
    try {
      val scanner = new GeneralScanner(uri, true)
      val parser = GeneralParser.create(scanner, false)
      val ast = parser.parse()
      Some(wrapAsCompileUnit(ast))
    }catch{
      case e : FileNotFoundException => 
        error(s"file($uri) not found")
        System.exit(0)
        None
      case e => 
        throw e
    }
  }
}

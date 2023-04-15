package com.silence.vmy

import com.silence.vmy.compiler.{GeneralParser, GeneralScanner}
import com.silence.vmy.evaluate.TreeEmulator

import java.io.FileNotFoundException

object ScalaMain  extends  App{
  def run_with_scanner_s(content: String, run: GeneralScanner => Unit ) = {
    try{
      val scanner = new GeneralScanner(content, false);
      run(scanner)
    }catch {
      case e: FileNotFoundException => e
    }
  }
  println("hello, emulator")
  run_with_scanner_s(
    "let a: Int = 1 * (2 + 3)",
    scanner => GeneralParser.create(scanner).parse().accept(new TreeEmulator(), null)
  )
  println("hello, emulator")
}
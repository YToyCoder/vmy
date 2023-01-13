package com.silence.vmy

object ScalaMain {
  def f(): Unit = {

    for( i <- List("AN","BM")
        if i(0) == 'A';
       el <- i
    ) println(s"scala ${i} - ${el}")

  }
}
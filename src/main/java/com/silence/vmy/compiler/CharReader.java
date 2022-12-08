package com.silence.vmy.compiler;

public interface CharReader extends AutoCloseable {
  CharReaders.CharInFile peek();
  CharReaders.CharInFile next();
  boolean empty();
}

package com.silence.vmy.compiler;

/**
 * LexcicalException
 */
public class LexicalException extends RuntimeException{
  // position in file
  final int pos;
  // file_name
  final String file_name;
  public LexicalException(String msg){
//    super(msg);
//    pos = -1;
//    file_name = "empty";
    this(-1, "", msg);
  }

  public LexicalException(int _pos, String _file_name, String msg){
    super(msg);
    pos = _pos;
    file_name = _file_name;
  }
}
package com.silence.vmy.compiler.deprecated;

import com.silence.vmy.compiler.TokenHistoryRecorder;

import java.util.List;

public interface Scanner {
  @Deprecated
  List<Token> scan(final String source);

  /**
   * preview the next token, do not remove it
   * @return {@link Token}
   */
  Token peek();

  /**
   * get the next token and remove it
   * @return {@link Token}
   */
  Token next();

  /**
   * check if there is next token
   * @return true if has token, else false
   */
  boolean hasNext();

  /**
   * register a {@link TokenHistoryRecorder} to record each used Token,
   * if want to this function full work,you need implement it by yourself.
   * @param historyRecorder {@link TokenHistoryRecorder}
   * @param force determine if replace the old TokenHistory when already exists a recorder, if true , then force replace it and return true, else if exists return false
   * @return if set successful return true, else return false
   */
  default boolean register(TokenHistoryRecorder historyRecorder, boolean force){
    return false;
  }

}

package com.silence.vmy.compiler;

/**
 * record the Token that Token has been used 
 */
public interface TokenHistoryRecorder{
  /**
   * record the use token to history
   * @param token {@link Token}
   * @return {@code this} for chain invoke
   */
  TokenHistoryRecorder record_to_history(Token token);
  /**
   * last record Token
   * @return {@link Token} if exist return , if is empty Throw RecordHistoryEmpty exception
   */
  Token last();

  /**
   * @param index
   * @return
   */
  Token get(int index);
  
  /**
   * check if the record is empty
   * @return  
   */
  boolean has_history(int index);

  boolean has_history();
}
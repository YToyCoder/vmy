package com.silence.vmy;

import com.silence.vmy.compiler.FixedSizeCapabilityTokenRecorder;
import com.silence.vmy.compiler.RecordHistoryEmptyException;
import com.silence.vmy.compiler.oldIR.Token;
import com.silence.vmy.compiler.OneCapabilityTokenRecorder;
import org.junit.Test;

import static org.junit.Assert.*;

public class TokenRecorder {

  @Test
  public void one_capability_token_recorder_test(){
    OneCapabilityTokenRecorder token_recorder = new OneCapabilityTokenRecorder();
    assertFalse("init recorder", token_recorder.has_history());
    assertThrows(RecordHistoryEmptyException.class, () -> {
      token_recorder.last();
    });
    token_recorder.record_to_history(new Token(0, ""));
    assertTrue("add one token", token_recorder.has_history());
  }

  @Test
  public void fixed_size_capability_token_recorder(){
    FixedSizeCapabilityTokenRecorder token_recorder = new FixedSizeCapabilityTokenRecorder(3);
    assertFalse("init recorder", token_recorder.has_history());
    assertThrows(RecordHistoryEmptyException.class, () -> {
      token_recorder.last();
    });
    Token token = new Token(-1, "");
    token_recorder.record_to_history(token);
    assertTrue("add one token", token_recorder.has_history());
    assertEquals(token, token_recorder.last());
    Token token1 = new Token(-1, "");
    token_recorder.record_to_history(token1);
    assertEquals(token1, token_recorder.last());
    assertEquals(token, token_recorder.get(1));
    assertTrue(token_recorder.has_history(0));
    assertTrue(token_recorder.has_history(1));
    assertTrue(token_recorder.has_history(2));
    Token token2 = new Token(-1, "");
    token_recorder.record_to_history(token2);
    assertTrue(token_recorder.has_history(2));
    assertEquals(token2, token_recorder.last());
  }
}

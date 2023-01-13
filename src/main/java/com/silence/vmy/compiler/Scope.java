package com.silence.vmy.compiler;

public interface Scope {
  CompilingPhaseVariable lookupMethod(String id);
  CompilingPhaseType lookupType(String id);
}

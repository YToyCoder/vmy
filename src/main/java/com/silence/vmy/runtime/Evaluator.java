package com.silence.vmy.runtime;

import com.silence.vmy.compiler.tree.Root;

public interface Evaluator {
    Object eval(Root tree);
}

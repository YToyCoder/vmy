package com.silence.vmy.compiler.transform;

import java.util.function.Function;

@FunctionalInterface
public interface Transformer<From, To> extends Function<From,To> {
}

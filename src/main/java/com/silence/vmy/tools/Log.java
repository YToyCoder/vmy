package com.silence.vmy.tools;

public abstract class Log {
  public void log(String msg) {
    System.out.println("[%s]: %s".formatted(this.getClass().getName(), msg));
  }
}

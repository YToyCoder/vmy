package com.silence.vmy.compiler.tree;

import java.util.List;

public interface Root extends Tree{
  Tree body();
  default String file_name() { return ""; }
  default List<ImportState> imports(){ return List.of(); }
  default List<ExportState> exports(){ return List.of(); }
}

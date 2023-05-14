package com.silence.vmy.compiler.tree;

import java.util.Objects;
import java.util.List;

public class ImportState implements Statement
{

  private String uri;
  private long position;
  private List<ImportExp> imports;
  private boolean asOne;
  private boolean isElems;

  public String uri() { return uri; }
  // import Module from "uri"
  public boolean isImportAsOne(){ return asOne; }
  // import { a, b } from "uri"
  public boolean isElementImport(){ return isElems; }
  public ImportExp oneImport() { return imports.size() > 0 ? imports.get(0) : null ; }
  public List<ImportExp> elemImport(){ return imports; }
  // alias => import Module as alias-name from "uri"
  // alias => import { a as alias-name, b } from "uri"
  // normal => import ModuleName from "uri"
  public record ImportExp(String alias, String name, boolean hasAlias,long position) {}

  private ImportState(String uri, long position, List<ImportExp> imports, boolean asOne, boolean isElems) {
    this.uri = uri;
    this.position = position;
    this.imports = imports;
    this.asOne = asOne;
    this.isElems = isElems;
  }

  public static ImportState create(String uri, ImportExp one, long position){
    return new ImportState(uri, position, List.of(one), true, false);
  }

  public static ImportState create(String uri, List<ImportExp> elems, long position)
  {
    return new ImportState(uri, position, elems, false, true);
  }

  public static ImportExp createImportExp(String name, long position)
  {
    return new ImportExp("", Objects.requireNonNull(name), false, position);
  }

  public static ImportExp createImportExp(String name, String alias, long position)
  {
    return new ImportExp(Objects.requireNonNull(alias), Objects.requireNonNull(name), true, position);
  }

  @Override public Tag tag() { return Tag.Import; }
  @Override
  public <R, T> R accept(TreeVisitor<R, T> visitor, T payload) {
    return visitor.visitImport(this, payload);
  }
  @Override
  public <T> Tree accept(TVisitor<T> visitor, T t) {
    if(visitor.enterImport(this, t))
      return visitor.leaveImport(this, t);
    return this;
  }
  @Override public long position() { return position; }
}

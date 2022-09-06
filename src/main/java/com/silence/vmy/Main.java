package com.silence.vmy;

import java.util.*;

public class Main {
  public static void main(String[] args) {
    String[] strings = handle_args(args);
    if(Objects.nonNull(strings) && strings.length > 0 )
    switch (strings[0]){
      case Repl:
        Eval.repl();
        break;
      case Run:
        Scripts.run(Arrays.copyOfRange(strings, 1, strings.length));
        break;
    }else{
      Utils.log("no file");
    }
  }

  private static String[] handle_args(String[] args){
    Map<String, Integer> string_index_mapper = new HashMap<>();
    for(int i=0; i < args.length; i++){
      string_index_mapper.put(args[i], i);
    }
    if(string_index_mapper.containsKey(Repl) && string_index_mapper.containsKey(Run))
      throw new RuntimeException("can't use -sh and -r both");
    if(string_index_mapper.containsKey(Repl))
      return new String[] {Repl};
    Integer index = string_index_mapper.get(Run);
    return Objects.isNull(index) ? null :
        string_index_mapper.entrySet().stream()
            .filter(entry -> entry.getValue() >= index)
            .sorted(Comparator.comparingInt(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .toList()
            .toArray(new String[0]);
  }

  private static final String Repl = "-sh";
  private static final String Run  = "-r";
  /**
   * string -> tokenize() ->
   *
   * "int a = 3;" -> ["int", "a", "=", "3", ";"]
   *
   * -> lexical
   *
   * ["int", "a", "=", "3", ";"]
   *
     *                                            =
   *       /                                                \
   *    {type: "type", identifier : "a"}            expression () : {type: literal, value: 3}
   *                                                      {1 + 3  * ( 4 + 5 )}
   */
}

package com.silence.vmy.compiler;

public class FixedSizeCapabilityTokenRecorder implements TokenHistoryRecorder {

  private final int MaxSize;
  private final Token[] tokens;
  private int head;
  private int tail;

  public FixedSizeCapabilityTokenRecorder(int max_size){
    MaxSize = max_size;
    tokens = new Token[max_size];
    reset();
  }

  private void append(Token token){
    if(isEmpty())
      tokens[(head = tail = 0)] = token;
    else {
      if(isFull())
        head = move_forward(head);
      tokens[tail = move_forward(tail)] = token;
    }
  }

  private int move_forward(int i){
    return (i + 1) % MaxSize;
  }

  private boolean isEmpty(){
    return head == -1;
  }

  private boolean isFull(){
    return move_forward(tail) == head;
  }

  private void reset() {
    head = tail = -1;
  }

  @Override
  public TokenHistoryRecorder record_to_history(Token token) {
    append(token);
    return this;
  }

  @Override
  public Token last() {
    return get(0);
  }

  @Override
  public Token get(int index) {
    if(index >= MaxSize)
      throw new IndexOutOfBoundsException("index is of bounds for max size " + MaxSize);
    else if(isEmpty())
      throw new RecordHistoryEmptyException("recorder is empty");
    return tokens[reverse_index(index)];
  }

  /**
   * [ 1, 2, 3, 4, 5 ]
   * @param index
   * @return
   */
  @Override
  public boolean has_history(int index) {
    final int reversed_index = reverse_index(index);
    return index >= 0 && index < MaxSize &&
        (
          /* arr[.. head <--> tail .. ] */
          (head <= tail && reversed_index >= head) ||
          /* arr[ -> tail ... head -> ]*/
          (head > tail && (reversed_index < MaxSize || reversed_index <= tail))
        );
  }

  private int reverse_index(int index){
    return (tail - index + MaxSize) % MaxSize;
  }

  @Override
  public boolean has_history() {
    return !isEmpty();
  }
}

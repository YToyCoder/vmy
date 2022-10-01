package com.silence.vmy;

import java.util.List;
import java.util.Objects;
import java.util.Stack;

public class VisitingEvaluator implements AST.Evaluator, NodeVisitor{
  Global _g = Global.getInstance();
  private Stack<Object> interval_op_val = new Stack<>();

  @Override
  public Object eval(AST.Tree tree) {
    if(tree instanceof AST.VmyAST ast){
      ast.root.accept(this);
      return get_from_stack();
    }else
      throw new EvaluatException("unrecognized AST");
  }

  private void put_stack(Object obj){
    interval_op_val.add(obj);
  }

  private Object get_from_stack(boolean remove){
    return remove ? interval_op_val.pop() : interval_op_val.peek();
  }

  private Object get_from_stack(){
    return get_from_stack(true);
  }

  @Override
  public void visitBinaryOperator(AST.BinaryOperatorNode node) {
    var op = node.OP;
    node.left.accept(this);
    var left = get_from_stack();
    node.right.accept(this);
    var right = get_from_stack();
    if(Objects.isNull(right) || Objects.isNull(left))
      throw new EvaluatException(op + " can't handle null object");
    BinaryOps b_op = BinaryOps.OpsMapper.get(op);
    if(Objects.isNull(b_op))
      throw new EvaluatException("op(" + op + ") not support!");
    try{
        Object result = Objects.nonNull(b_op) ?
            b_op.apply(TreeEvaluatorHelper.get_value(left, _g), TreeEvaluatorHelper.get_value(right, _g)) :
            null;
        put_stack(result);
    }catch (OpsException e){
      throw new ASTProcessingException(String.format("%s : left value - %s, right value - %s", e.getMessage(), left, right));
    }
  }

  @Override
  public void visitBlockNode(AST.BlockNode node) {
    for(AST.ASTNode sub : node.process){
      sub.accept(this);
      get_from_stack(); // drop result
    }
    put_stack(null);
  }

  @Override
  public void visitWhileLoop(AST.WhileLoop node) {
    node.condition.accept(this);
    boolean condition = (boolean) get_from_stack();
    while (condition){
      node.body.accept(this);
      node.condition.accept(this);
      condition = (boolean) get_from_stack();
    }
    put_stack(null);
  }

  @Override
  public void visitAssignment(AST.AssignNode node) {
    // evaluate expression
    node.expression.accept(this);
    String variable_name;
    final Object value = get_from_stack(false);
    if(node.variable instanceof AST.DeclareNode declaration){
      declaration.accept(this);
      variable_name = (String) get_from_stack();
    }else if(node.variable instanceof AST.IdentifierNode identifier) {
      variable_name = identifier.value;
    }else
      throw new EvaluatException("not support type node in left of assignment");
    // keep result
    if(TreeEvaluatorHelper.check_if_value_can_be_assign_to(
        variable_name,
        value,
        _g)){
      TreeEvaluatorHelper.assign_to(
          variable_name,
          TreeEvaluatorHelper.get_variable(variable_name, _g),
          value,
          _g);
    }
  }

  @Override
  public void visitDeclareNode(AST.DeclareNode node) {
    Object expression = get_from_stack(false);
    VmyType expression_type = Utils.get_obj_type(expression);
    final VmyType declaration_type = Objects.isNull(node.type) ? expression_type : Utils.to_type(node.type);
    TreeEvaluatorHelper.can_assign(declaration_type, expression_type);
    put_stack(node.identifier.value);
    var variable = Runtime.declare_variable(
        _g,
        node.identifier.value,
        declaration_type,
        Utils.is_mutable(node.declare)
    );
  }

  @Override
  public void visitIdentifierNode(AST.IdentifierNode node) {
    put_stack(TreeEvaluatorHelper.get_variable(node.value, _g));
  }

  @Override
  public void visitLiteralNode(AST.LiteralNode node) {
    put_stack(node.val());
  }

  @Override
  public void visitCallNode(AST.CallNode node) {
    put_stack(FunctionSupport.call(
        node.identifier,
        node.params.elements.stream()
            .map(param -> {
              param.accept(this);
              return TreeEvaluatorHelper.get_value(get_from_stack(), _g);
            })
            .toList()
    ));
  }

  @Override
  public void visitIfElse(AST.IfElse node) {
    AST.ConditionNode the_if = node.TheIf;
    the_if.condition.accept(this);
    if((boolean) get_from_stack()){
      the_if.body.accept(this);
    }else if(!eval_elif(node.Elif) && Objects.nonNull(node.Else)){
      node.Else.accept(this);
    }
  }

  boolean eval_elif(List<AST.ConditionNode> ifEls){
    for(AST.ConditionNode el : ifEls){
      el.condition.accept(this);
      if((boolean) get_from_stack()){
        el.body.accept(this);
        return true;
      }
    }
    return false;
  }
}

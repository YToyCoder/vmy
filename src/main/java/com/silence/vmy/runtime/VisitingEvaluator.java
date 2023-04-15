package com.silence.vmy.runtime;

import com.silence.vmy.compiler.AST;
import com.silence.vmy.compiler.oldIR.*;
import com.silence.vmy.compiler.visitor.ASTProcessingException;
import com.silence.vmy.compiler.visitor.NodeVisitor;
import com.silence.vmy.compiler.tree.*;
import com.silence.vmy.tools.Utils;

import java.util.List;
import java.util.Objects;
import java.util.Stack;

public class VisitingEvaluator implements Evaluator, NodeVisitor {
  Frame _g = Global.getInstance();
  private final Stack<Object> interval_op_val = new Stack<>();
  private final RuntimeContext runtimeContext = new LinkedListRuntimeContext();
  private Object return_value;

  @Override
  public Object eval(Root tree) {
    if(tree instanceof AST.VmyAST ast){
      _g = runtimeContext.new_frame();
      if(Objects.isNull(ast.root))
        return null;
      ast.root.accept(this);
      return get_from_stack();
    }else
      throw new EvaluateException("unrecognized AST");
  }

  private void put_stack(Object obj){ interval_op_val.add(obj); }
  private Object get_from_stack(boolean remove){ return remove ? interval_op_val.pop() : interval_op_val.peek(); }
  private Object get_from_stack(){ return get_from_stack(true); }

  @Override
  public void visitBinaryOperator(BinaryOperatorNode node) {
    var op = node.op();
    node.left().accept(this);
    var left = get_from_stack();
    node.right().accept(this);
    var right = get_from_stack();
    if(Objects.isNull(right) || Objects.isNull(left))
      throw new EvaluateException(op + " can't handle null object");
    BinaryOps b_op = BinaryOps.OpsMapper.get(op);
    if(Objects.isNull(b_op))
      throw new EvaluateException("op(" + op + ") not support!");
    try{
        Object result = b_op.apply(left, right);
        put_stack(result);
    }catch (OpsException e){
      throw new ASTProcessingException(String.format("%s : left value - %s, right value - %s", e.getMessage(), left, right));
    }
  }

  @Override
  public void visitBlockNode(BlockNode node) {
    for(Tree sub : node.process()){
      sub.accept(this);
      get_from_stack(); // drop result
    }
    put_stack(null);
  }

  @Override
  public void visitWhileLoop(WhileLoop node) {
    node.condition().accept(this);
    boolean condition = (boolean) get_from_stack();
    while (condition){
      node.body().accept(this);
      node.condition().accept(this);
      condition = (boolean) get_from_stack();
    }
    put_stack(null);
  }

  @Override
  public void visitAssignment(AssignNode node) {
    // evaluate expression
    node.expression().accept(this);
    String variable_name;
    final Object value = get_from_stack(false);
    if(node.variable() instanceof DeclareNode declaration){
      declaration.accept(this);
      variable_name = (String) get_from_stack();
    }else if(node.variable() instanceof IdentifierNode identifier) {
      variable_name = identifier.val();
    }else
      throw new EvaluateException("not support type node in left of assignment");
    // keep result
    if(
        TreeEvaluatorHelper.check_if_value_can_be_assign_to(
          variable_name,
          value,
          runtimeContext.current_frame()
        )
    ){
      TreeEvaluatorHelper.assign_to(
          variable_name,
          TreeEvaluatorHelper.get_variable(variable_name, runtimeContext.current_frame()),
          value,
          runtimeContext.current_frame()
      );
      put_stack(value);
    }
  }

  @Override
  public void visitDeclareNode(DeclareNode node) {
    Object expression = get_from_stack(false);
    VmyType expression_type = Utils.get_obj_type(expression);
    final VmyType declaration_type = Objects.isNull(node.type()) ? expression_type : Utils.to_type(node.type());
    TreeEvaluatorHelper.can_assign(declaration_type, expression_type);
    put_stack(node.identifier().val());
    com.silence.vmy.runtime.Runtime.declare_variable(
        runtimeContext.current_frame(),
        node.identifier().val(),
        declaration_type,
        Utils.is_mutable(node.declare())
    );
  }

  @Override
  public void visitIdentifierNode(IdentifierNode node) {
    put_stack(
        TreeEvaluatorHelper.get_value(
            TreeEvaluatorHelper.get_variable(node.val(), runtimeContext.current_frame()),
            runtimeContext.current_frame()
        )
    );
  }

  @Override
  public void visitLiteralNode(LiteralNode node) { put_stack(node.val()); }

  @Override
  public void visitCallNode(CallNode node) {
    return_value = null; // reset return_value
    if(/* builtin call */ FunctionSupport.is_builtin_func(node.identifier())){
      put_stack(FunctionSupport.call(
          node.identifier(),
          node.params().elements().stream()
              .map(param -> {
                param.accept(this);
                return TreeEvaluatorHelper.get_value(get_from_stack(), runtimeContext.current_frame());
              })
              .toList()
      ));
    } else { /* declared Function */
      FunctionSupport.ASTFunction function = (FunctionSupport.ASTFunction) TreeEvaluatorHelper.get_value(
          TreeEvaluatorHelper.get_variable(node.identifier(), runtimeContext.current_frame()),
          runtimeContext.current_frame()
      );
      if(Objects.isNull(function)){
        throw new VmyRuntimeException("can't find function %s".formatted(node.identifier()));
      }
      runtimeContext.new_frame();
      List<Tree> params = node.params().elements();
      int params_len = Math.min( params.size(),  /* last one is type of return value */ function.func().params().size() - 1);
      for(int i=0; i<params_len; i++){
        params.get(i).accept(this);
        Object value = get_from_stack(true);
        DeclareNode param_declaration = function.func().params().get(i);
        com.silence.vmy.runtime.Runtime.declare_variable(
            runtimeContext.current_frame(),
            param_declaration.identifier().val(),
            Utils.to_type(param_declaration.type()),
            value,
            false
        );
      }
      function.func().body().accept(this);
      runtimeContext.exitCurrentFrame();
      put_stack(return_value);
    }
  }

  @Override
  public void visitIfElse(IfElse node) {
    ConditionNode the_if = node.theIf();
    the_if.condition().accept(this);
    if((boolean) get_from_stack()){
      the_if.body().accept(this);
    }else if(!eval_elif(node.elif()) && Objects.nonNull(node._else())){
      node._else().accept(this);
    }
  }

  @Override
  public void visitFunction(FunctionNode node) {
    String function_name = node.name();
    put_stack(
      Runtime.declare_variable(
        runtimeContext.current_frame(),
        function_name,
        VmyTypes.BuiltinType.Function,
        FunctionSupport.create_ast_function(node),
        false
      )
    );
  }

  @Override
  public void visitReturn(Return node) {
    if(Objects.isNull(node.val()))
      return_value = null;
    else {
      node.val().accept(this);
      return_value = get_from_stack();
    }
  }

  @Override
  public void visitEmpty(EmptyNode node) { put_stack(null); }


  boolean eval_elif(List<ConditionNode> ifEls){
    for(ConditionNode el : ifEls){
      el.condition().accept(this);
      if((boolean) get_from_stack()){
        el.body().accept(this);
        return true;
      }
    }
    return false;
  }
}

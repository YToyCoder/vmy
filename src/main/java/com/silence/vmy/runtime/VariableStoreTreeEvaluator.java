package com.silence.vmy.runtime;

import com.silence.vmy.compiler.AST;
import com.silence.vmy.compiler.deprecated.*;
import com.silence.vmy.compiler.visitor.ASTProcessingException;
import com.silence.vmy.compiler.tree.*;
import com.silence.vmy.tools.Utils;

import java.util.List;
import java.util.Objects;

public class VariableStoreTreeEvaluator implements Evaluator {
    private Global _g = Global.getInstance();

    @Override
    public Object eval(Root tree) {
        if (tree instanceof AST.VmyAST ast) {
            return eval_sub(ast.root);
        } else
            throw new EvaluateException("unrecognized AST");
    }

    /**
     * evaluate each node of the tree
     *
     * @param node {@link Tree}
     * @return the node evaluating result , like
     */
    Object eval_sub(Tree node) {

        if (node instanceof ValNode val) {
            return val.val();
        } else if (node instanceof BlockNode block) {

            List<Tree> nodes = block.process();
            for (Tree sub : nodes) {
                eval_sub(sub);
            }
            return null;

        } else if (node instanceof BinaryOperatorNode common) {

            return binary_op_call(
                    common.op(),
                    eval_sub(common.left()),
                    eval_sub(common.right())
            );

        } else if (node instanceof AssignNode assignment) {

            return handle_assignment_node(assignment);

        } else if (node instanceof DeclareNode declaration) {
            return Utils.variable_with_name(
                    declaration.identifier().val(),
                    Runtime.declare_variable(
                            _g,
                            declaration.identifier().val(),
                            Utils.to_type(declaration.type()),
                            Utils.is_mutable(declaration.declare())
                    )
            );
        } else if (node instanceof IdentifierNode identifier) {

            try {
                return get_variable(identifier.val());
            } catch (Exception e) {
                Utils.error(e.getMessage());
                return null;
            }

        } else if (node instanceof LiteralNode literal) {
            return literal.val();
        } else if (node instanceof CallNode call) {
            return do_call(call);
        } else if (node instanceof WhileLoop while_loop) {
            while ((boolean) eval_sub(while_loop.condition())) {
                eval_sub(while_loop.body());
            }
            return null;
        } else if (node instanceof IfElse ifElse) {
            do_evaluate_if_else(ifElse);
            return null;
        } else
            throw new EvaluateException("unrecognizable AST node");
    }

    // compare type
    // check if variable can be assigned
    Object handle_assignment_node(AssignNode assignment) {
        Object expression = eval_sub(assignment.expression());
        VmyType expression_type = Utils.get_obj_type(expression);

        Object expression_value = get_value(expression);
        if (assignment.variable() instanceof IdentifierNode identifier) {
            try {
                Runtime.VariableWithName identifier_variable = get_variable(identifier.val());
                can_assign(identifier_variable, expression);
                assign_to(identifier_variable.name(), identifier_variable, expression_value);
            } catch (Exception e) {
                Utils.error(e.getMessage());
            }
        } else if (assignment.variable() instanceof DeclareNode declaration) {

            final VmyType declaration_type = Objects.isNull(declaration.type()) ? expression_type : Utils.to_type(declaration.type());
            can_assign(declaration_type, expression_type);
            assign_to(
                    declaration.identifier().val(),
                    Runtime.declare_variable(
                            _g,
                            declaration.identifier().val(),
                            declaration_type,
                            Utils.is_mutable(declaration.declare())
                    ),
                    expression_value
            );
        }
        return expression_value;
    }

    /**
     * handle the binary operation like : 1 + 2, 2 * 4
     *
     * @param op    operation
     * @param left  operation left side
     * @param right operation right side
     * @return call result , like : 1 + 2 -> 3
     */
    Object binary_op_call(String op, Object left, Object right) {
        if (Objects.isNull(right) || Objects.isNull(left))
            throw new EvaluateException(op + " can't handle null object");
        BinaryOps b_op = BinaryOps.OpsMapper.get(op);
        if (Objects.isNull(b_op))
            throw new EvaluateException("op(" + op + ") not support!");
        return Objects.nonNull(b_op) ? b_op.apply(get_value(left), get_value(right)) : null;
    }

    /**
     * if type of {@code expression_type} can be assigned to type of {@code variable_type}
     *
     * @param variable_type   variable type
     * @param expression_type expression type
     * @return true else throw {@link ASTProcessingException}
     */
    boolean can_assign(VmyType variable_type, VmyType expression_type) {
        if (!Utils.equal(variable_type, expression_type))
            throw new ASTProcessingException("type " + expression_type + " can not be assigned to type " + variable_type);
        return true;
    }

    /**
     * check the declaration , if it's const variable , it will not be assigned, then check if the type is match
     *
     * @param variable {@link Runtime.VariableWithName}
     * @param value    assigned value
     */
    void can_assign(Runtime.VariableWithName variable, Object value) {
        if (!variable.mutable())
            throw new EvaluateException("const variable (let) can't be assigned : " + variable.name());
        can_assign(variable.getType(), Utils.get_obj_type(value));
    }

    /**
     * evaluate the code block if-else
     *
     * @param if_else {@link IfElse}
     */
    void do_evaluate_if_else(IfElse if_else) {
        ConditionNode the_if = if_else.theIf();
        if ((boolean) eval_sub(the_if.condition())) {
            eval_sub(the_if.body());
        } else if (!eval_elif(if_else.elif()) && Objects.nonNull(if_else._else())) {
            eval_sub(if_else._else());
        }
    }

    boolean eval_elif(List<ConditionNode> _ifEls) {
        for (ConditionNode _el : _ifEls) {
            if ((boolean) eval_sub(_el.condition())) {
                eval_sub(_el.body());
                return true;
            }
        }
        return false;
    }

    /**
     * handle a call expression like:
     * <p>
     * print(1, "string")
     *
     * @param call_node
     * @return
     */
    Object do_call(CallNode call_node) {
        // get params and check if the type is matched
        // get the called function type
        return FunctionSupport.call(
                call_node.identifier(),
                call_node.params().elements().stream()
                        .map(param -> get_value(eval_sub(param)))
                        .toList()
        );
    }

    Runtime.VariableWithName get_variable(String name) {
        Runtime.Variable variable = _g.local(name);
        if (Objects.isNull(variable))
            throw new EvaluateException("variable " + name + " haven't declared!");
        return Utils.variable_with_name(name, variable);
    }

    // assign value to variable
    void assign_to(String variable_name, Runtime.Variable variable, Object value) {
        _g.put(variable_name, variable, value);
    }


    /**
     * get the value of an object
     * <p>
     * classified to 2 type
     * <p>
     * 1. Variable type : get the value that the variable point at
     * <p>
     * 2. it's a value : return
     *
     * @param obj
     * @return
     */
    Object get_value(Object obj) {
        if (obj instanceof Runtime.VariableWithName variable) {
            return Runtime.get_value(variable.name(), _g);
        }
        return obj;
    }

}

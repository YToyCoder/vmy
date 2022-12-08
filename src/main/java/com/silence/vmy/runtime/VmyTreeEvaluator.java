package com.silence.vmy.runtime;

import com.silence.vmy.compiler.AST;
import com.silence.vmy.compiler.deprecated.*;
import com.silence.vmy.compiler.tree.*;

import java.util.Objects;

public class VmyTreeEvaluator implements Evaluator {
    private Global _g = Global.getInstance();

    @Override
    public Object eval(Root tree) {
        if (tree instanceof AST.VmyAST ast) {
            return evalsub(ast.root);
        } else
            throw new EvaluateException("unrecognized AST");
    }

    Object evalsub(Tree node) {
        if (node instanceof ValNode val) {
            return val.val();
        } else if (node instanceof BinaryOperatorNode common) {
            Object left = evalsub(common.left());
            Object right = evalsub(common.right());
            if (Objects.isNull(right) || Objects.isNull(left))
                throw new EvaluateException(common.op() + " can't handle null object");
            BinaryOps op = BinaryOps.OpsMapper.get(common.op());
            if (Objects.isNull(op))
                throw new EvaluateException("op(" + common.op() + ") not support!");
            return Objects.nonNull(op) ? op.apply(getValue(left), getValue(right)) : null;
        } else if (node instanceof AssignNode assignment) {
            String variable_name = (String) evalsub(assignment.variable());
            Object value = getValue(evalsub(assignment.expression()));
            if (value instanceof LiteralNode string_literal)
                value = string_literal.val();
            findAndPut(variable_name, value);
            return value;
        } else if (node instanceof DeclareNode declaration) {
            _g.put(declaration.identifier().val(), null);
            return declaration.identifier().val();
        } else if (node instanceof IdentifierNode identifier) {
            return identifier.val();
        } else if (node instanceof LiteralNode literal) {
            return literal;
        } else if (node instanceof BlockNode block) {
            for (Tree el : block.process()) {
                evalsub(el);
            }
            return null;
        } else
            throw new EvaluateException("unrecognizable AST node : " + node.getClass().getName());
    }

    Object getValue(Object obj) {
        if (obj instanceof String obj_name) {
            if (!_g.exists(obj_name))
                throw new EvaluateException(String.format("variable (%s) not exists", obj_name));
            return _g.get(obj_name);
        }
        return obj;
    }

    /**
     * @param _name
     * @param _value
     */
    void findAndPut(String _name, Object _value) {
        if (!_g.exists(_name))
            throw new EvaluateException(_name + " haven't declared!");
        _g.put(_name, _value);
    }

}

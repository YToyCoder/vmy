package com.silence.vmy.compiler.tree;

import com.silence.vmy.compiler.visitor.NodeVisitor;

public interface Tree {

    // old version
    default void accept(NodeVisitor visitor){
        accept(visitor);
    }

    // new version
    <R,T> R accept(TreeVisitor<R,T> visitor, T payload);
    <T> Tree accept(TVisitor<T> visitor, T t);

    long position();

    Tag tag();

    enum Tag{
        Root,
        // ops
        Add,
        AddEqual, // +=
        Concat, // ++
        Sub, // -
        SubEqual, // -=
        Multi, // *
        MultiEqual, // *=
        Div, // /
        DivEqual, // /=
        Assign,
        // literal
        IntLiteral,
        StringLiteral,
        DoubleLiteral,
        FunctionLiteral,
        //
        Fun,
        VarDecl,
        // 
        Param,
        Id,
        TypeDecl,
        CallExpr
    };

    static String opTag2String(Tag tag){
        return switch (tag){
            case Add -> "+";
            case AddEqual -> "+=";
            case Sub -> "-";
            case Div -> "/";
            case SubEqual -> "-=";
            case Multi -> "*";
            case Assign -> "=";
            case MultiEqual -> "*=";
            case DivEqual -> "/=";
            case Concat -> "++";
            default -> "";
        };
    }
}

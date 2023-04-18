package com.silence.vmy.compiler.tree;

import com.silence.vmy.compiler.visitor.NodeVisitor;

public interface Tree {

	// old version
	default void accept(NodeVisitor visitor){ accept(visitor); }
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

		/* comparations */
		Equal, // ==
		NotEqual, // !=
		Less, // <
		Greater, // >
		Le, // <=
		Ge, // >=

		// literal
		IntLiteral,
		StringLiteral,
		DoubleLiteral,
		FunctionLiteral,
		// obj
		Arr,
		//
		Param,
		Id,
		TypeDecl,
		CallExpr,

		// reserved keys
		/** declaration */
		Fun,
		VarDecl,
		/** condition */
		If,
		Elif,
		Else,
		/** loop */
		For
	};

	static String opTag2String(Tag tag){
		return switch (tag){
			/* operator */
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
			/* condition */
			case If -> "if";
			case Elif -> "elif";
			case Else -> "else";
			/* loop */
			case For -> "for";
			/* comparations */
			case Equal -> "=="; // ==
			case NotEqual -> "!="; // !=
			case Less -> "<"; // <
			case Greater -> ">"; // >
			case Le -> "<="; // <=
			case Ge -> ">="; // >=
			default -> ">> no mapping tag <<";
		};
	}
}

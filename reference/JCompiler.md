## 1. 语法生成式

#function ident
```txt
Ident = Identifier
```

#function qualident
```txt
Gualident = Ident { Dot [Annotations] Ident }
```

#function  literal
```txt
 Literal = 
 	Intliteral
   | LongLiteral
   | FloatLiteral
   | DoubleLiteral
   | CharLiteral
   | StringLiteral
   | True
   | Flase
   | Null
```

#function term
```txt
Expression = Expression1 [ExpressionRest]
ExpressionRest = [AssignmentOperator Expression1]
AssignmentOperator = "=" | "+=" | "-=" | "`*=`" | "/=" 
				   | "&=" | "|=" | "^=" 
				   | "%=" | "<<=" | ">>=" | ">>>="
Type = Type1
TypeNoParams = TypeNoParams1
StatementExpression = Expression
ConstantExpression = Expression
```

#function term1
```txt
Expression1 = Expression2 [Expression1Rest]
Type1 = Type2
TypeNoParams1 = TypeNoParams
```


#function term1Rest
```txt
Expression1Rest = ["?" Expression ":" Expression1]
```

#function term2
```txt
Expression2 = Expression3 [Expression2Rest]
Type2 = Type3
TypeNoParams2 = TypeNoParams3
```

#function term2Rest
```txt
Expression2Rest = {infixop Expression3} 
				| Expression3 instanceof Type 
				| Expression3 instanceof Pattern

infixop = "||" 
		| "&&" 
		| "|" 
		| "^" 
		| "&" 
		| "`==`" | "!=" 
		| "<" | ">" | "<=" | ">=" 
		| "<<" | ">>" | ">>>" 
		| "+" | "-" 
		| "`*`" | "/" | "%" 
```

#function term3
```txt
Expression3 = PrefixOp Expression3 
  | "(" Expr | TypeNoParams ")" Expression3 
  | Primary {Selector} { PostfixOp }

Primary = "(" Expression ")"
  | Literal 
  | [TypeArguments] this [Arguments]
  | [TypeArguments] super SuperSuffix
  | NEW `[TypeArguments]` Creator
  | "(" Arguments ")" "->" ( Expression | Block)
  | Ident "->" (Expression | Block )
  | [Annotations] Ident { "." [Annotations] Ident }
  | Expression3 MemberReferenceSuffix
    [ [Annotations] "[" ( "]" BracketsOpt "." CLASS | Expression"]" )
    | Arguments
    | "."a ( CLASS | THIS | [TypeArguments] SUPER Arguments | NEW [TypeArguments] InnerCreator )
    ]
  | BasicType BracketOpt "." CLASS

PrefixOp      = "++" | "--" | "!" | "~" | "+" | "-"

PostfixOp     = "++" | "--"

Type3         = Ident { "." Ident } [TypeArguments] { TypeSelector } BracketsOpt 
              | BasicType
  
TypeNoParams3 = "." [TypeArguments] SUPER SuperSuffix
              | "." THIS
              | "." [TypeArguments] SUPER SuperSuffix
              | "." NEW [TypeArguments] InnerCreator
              | "[" Expression "]"
  
TypeSelector  = "." Ident [TypeArguments]

SuperSuffix   = Arguments | "." Ident [Arguments]
```

#function superSuffix
```txt
SuperSuffix = Arguments | "." [TypeArguments] Ident [Arguments]
```

#function basicType
```txt
BasicType = Byte | SHORT | Char | Int | Long | Float | Double | Boolean
```

#function argumentsOpt
```txt
ArgumentsOpt = [ Arguments ] 
```

#function arguments
```txt
Arguments = "(" [Expression] { COMMA Expression }] ")"
```

#function typeArgumentsOpt
```txt
TypeArgumentsOpt = [ TypeArguments ]
```

#function typeArguments 
```txt
TypeArguments = "<" TypeArgument { "," TypeArgument } ">"
```

#function typeArgument
```txt
TypeArgument = Type
			 | [Annotations] "?"
			 | [Annotations] "?" EXTENDS Type {"&" Type}
			 | [Annotations] "?" SUPER Type
```

#function bracketsOpt
```txt
BracketOpt = { [Annotations] "[" "]" }*
BracketOpt = [ "[" "]" { [Annotations] "[" "]"} ]
```

#function bracketSuffix
```txt
BracketsSuffixExpr = "." CLASS
BracketsSuffixType = 
```

#function memberReferenceSuffix
```txt
MemberReferenceSuffix = "::" [TypeArguments] Ident
                      | "::" [TypeArguments] "new"
```

#function creator
```txt
Creator = [Annotations] Qualident [TypeArguments] ( ArrayCreatorRest | ClassCreatorRest )
```

#function innerCreator
```txt
InnerCreator = [Annotations] Ident [TypeArguments] ClassCreatorRest
```

#function arrayCreatorRest
```txt
ArrayCreatorRest = [Annotations] "[" ( "]" BracketsOpt ArrayInitializer
                 | Expression "]" {[Annotations]  "[" Expression "]"} BracketsOpt )
```

#function classCreatorRest
```txt
ClassCreatorRest = Arguments [ClassBody]
```

#function arrayInitializer
```txt
ArrayInitializer = "{" [VariableInitializer {"," VariableInitializer}] [","] "}"
```

#function variableInitializer
```txt
VariableInitializer = ArrayInitializer | Expression
```

#function parExpression
```txt
ParExpression = "(" Expression ")"
```

#function block
```txt
Block = "{" BlockStatements "}"
```

#function blockStatements
```txt
BlockStatements = { BlockStatement }
BlockStatement  = LocalVariableDeclarationStatement
                | ClassOrInterfaceOrEnumDeclaration
                | [Ident ":"] Statement
LocalVariableDeclarationStatement
                = { FINAL | '@' Annotation } Type VariableDeclarators ";"
```

#function parseSimpleStatement
```
Statement =
       Block
     | IF ParExpression Statement [ELSE Statement]
     | FOR "(" ForInitOpt ";" [Expression] ";" ForUpdateOpt ")" Statement
     | FOR "(" FormalParameter : Expression ")" Statement
     | WHILE ParExpression Statement
     | DO Statement WHILE ParExpression ";"
     | TRY Block ( Catches | [Catches] FinallyPart )
     | TRY "(" ResourceSpecification ";"opt ")" Block [Catches] [FinallyPart]
     | SWITCH ParExpression "{" SwitchBlockStatementGroups "}"
     | SYNCHRONIZED ParExpression Block
     | RETURN [Expression] ";"
     | THROW Expression ";"
     | BREAK [Ident] ";"
     | CONTINUE [Ident] ";"
     | ASSERT Expression [ ":" Expression ] ";"
     | ";"
```

#function catchClause
```
CatchClause     = CATCH "(" FormalParameter ")" Block
```

#function switchBlockStatementGroup
```
SwitchBlockStatementGroups = { SwitchBlockStatementGroup }
SwitchBlockStatementGroup = SwitchLabel BlockStatements
SwitchLabel = CASE ConstantExpression ":" | DEFAULT ":"
```

#function moreStatementExpressions
```
MoreStatementExpressions = { COMMA StatementExpression }
```

## 2 产生式在学界的主流写法-BNF

```
<>::=<>|<>|<>
<>::=<><><>|<><>
<>::=<>|<>|<>
<>::=""|""|""
```
- <>表示该结构非终端
- ""表示该结构为终端
- | 表示或的逻辑

## 3 BNF写法的升级版EBNF

去掉了表示非终端结构的`<>`, 增加了`{}`和`[]`
{}表示该结构可一个可多个
`[]`表示该结构可有可没有

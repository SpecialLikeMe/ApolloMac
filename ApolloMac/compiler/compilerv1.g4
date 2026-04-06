grammar compilerv1;

@lexer::members {
  private boolean expectInclusive = false;
}

program      : (importStmt | include | function | macro | class | struct | interface | init | lambda | instance | instancepush | memberaccess ';' | nativemode | syscallStmt)* EOF ;
importStmt   : 'extern' (STRING | importPath | headerPath) ('-cpp')* ';'? ;
instance     : INSTANCE_MODE 'instance' ID ('=' instanceValue)? ';' ;
instancepush : ID '.' 'push' '(' instanceValue ')' ';' ;
memberaccess : ID '.' (functionCall|ID) ;
instanceValue: ID LBRACE args? RBRACE ;
importPath   : (ID | '*') ('.' (ID | '*'))* ;
headerPath   : headerPart (('/' | '\\') headerPart)+ ;
headerPart   : ID (('.' | '-') ID)* ;
function     : returnType ID '(' params? ')' block ;
method       : ANNOT_OVERRIDE? CLSTYPE? (STATIC? VIRTUAL? returnType ID '(' params? ')' block
             | '__construct' '(' params? ')' block
             | '__destruct' '(' ')' block) ;
field        : CLSTYPE? typeRef ID ';' ;
params       : param (',' param)* ;
param        : typeRef? ID ;
block        : LBRACE (statement | returnStmt)* RBRACE ;
classBody    : LBRACE classMember* RBRACE ;
structBody   : LBRACE structMember* RBRACE ;
classMember  : method | field | class | struct ;
structMember : method | field | class | struct ;
print        : 'sys' '.' ('stdout' | 'println') '(' expression ')' ';' ;
nativemode   : ASYNC ('-' OVERRIDE)? INCLUSIVE (NATIVE | ID) ';' ;
syscallStmt  : SYSCALL ('-' ALL)? ';' ;
pointer      : typeRef '*' ID '_' '&' ID '(' ')' ';' ;
include      : 'extern' '{' importPath '}' ';'? ;
returnType   : 'void' | typeRef ;
typeRef      : genericType
             | functionType
             | TYPE
             | FTYPE
             | ID ;
genericType  : ID '<' typeRef (',' typeRef)? '>' ;
functionType : 'fn' '<' returnType '(' functionTypeArgs? ')' '>' ;
functionTypeArgs : typeRef (',' typeRef)* ;
macro        : 'extern [&macro]' ID '(' params? ')' block ;
stdin        : 'sys' '.' 'stdin(' ID ')' ';' ;
lambda       : typeRef ID '=' 'lmd->' lambdaDefinition ;
lambdaDefinition : function | lambdaLiteral ;
lambdaLiteral : returnType? '(' params? ')' block ;
statement    : pointer
             | assignment
             | lambda
             | functionCall ';'
             | memberaccess ';'
             | ifStatement
             | whileStatement
             | init
             | instance
             | instancepush
             | nativemode
             | syscallStmt
             | class
             | struct
             | interface
             | print
             | stdin
             | block ;

ifStatement    : IF '(' expression ')' block (ELSE block)? ;
whileStatement : WHILE '(' expression ')' block ;
assignment     : assignTarget '=' expression ';' ;
assignTarget   : ID ('[' accessKey ']')* ;
accessKey      : expression
               | APND ;
init           : typeRef ID ( '=' expression )? ';' ;

expression     : orExpr ;

orExpr         : andExpr ( '||' andExpr )* ;

andExpr        : equalityExpr ( '&&' equalityExpr )* ;

equalityExpr   : relationalExpr ( ( '==' | '!=' ) relationalExpr )* ;

relationalExpr : addExpr ( ( '<' | '>' | '<=' | '>=' ) addExpr )* ;

addExpr        : multExpr (('+' | '-') multExpr)* ;

multExpr       : primary (('*' | '/' | '%') primary)* ;

primary        : INT
               | STRING
               | functionCall
               | indexedAccess
               | compositeLiteral
               | instanceValue
               | ID
               | '(' expression ')'
               ;
indexedAccess  : ID ('[' accessKey ']')+ ;
compositeLiteral : '<' expression (',' expression)+ '>' ;
functionCall   : ID '(' args? ')' ;
args           : expression (',' expression)* ;
returnStmt     : 'return' expression? ';' ;
class          : 'class' ID inheritanceClause? classBody ;
struct         : 'struct' ID inheritanceClause? structBody ;
interface      : 'itr' ID inheritanceClause? '{' virtualMethod* '}' ;
inheritanceClause : '*' inheritedType (';' inheritedType)* ;
inheritedType  : CLSTYPE? typeRef ;
virtualMethod  : VIRTUAL returnType ID '(' params? ')' ';' ;

IF       : 'if' ;
ELSE     : 'else' ;
WHILE    : 'while' ;
TYPE     : 'i32' | 'i64' | 'str' | 'f64' ;
FTYPE    : 'int' | 'short' | 'long' | 'float' | 'double' ;
CLSTYPE  : 'public' | 'private' ;
STATIC   : 'static' ;
VIRTUAL  : 'virtual' ;
INSTANCE_MODE : 'crt' | 'staticx' ;
ANNOT_OVERRIDE : '@Override' ;
ASYNC    : 'async' { expectInclusive = true; } ;
OVERRIDE : 'override' ;
SYSCALL  : 'syscall' ;
ALL      : 'recursive' ;
APND     : 'apnd' ;
ID       : [a-zA-Z_][a-zA-Z0-9_]* ;
NATIVE   : 'cpp' | 'c' | 'rs' | 'rust' | 'java' | 'cs' | 'csharp' | 'py' | 'python' | 'js' | 'javascript' | 'ts' | 'typescript' | 'go' | 'golang' | 'php' | 'rb' | 'ruby' | 'kt' | 'kotlin' ;
INCLUSIVE
    : {expectInclusive}? '{' INCLUSIVE_CONTENT* '}'
      {
          setText(getText().substring(1, getText().length() - 1));
          expectInclusive = false;
      }
    ;

LBRACE   : '{' ;
RBRACE   : '}' ;

fragment INCLUSIVE_CONTENT
    : '{' INCLUSIVE_CONTENT* '}' 
    | ~[{}]                     
    ;
STRING   : '"' .*? '"' ;
INT      : [0-9]+ ;
WS       : [ \t\r\n]+ -> skip ;
COMMENT  : '//' .*? '\n' -> skip ;

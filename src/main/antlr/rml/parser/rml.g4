grammar rml;

@header {
    package rml.parser;
}

spec: evtypeDecl* texpDecl+ ;
evtypeDecl: evtype NOT? 'matches' evtype ('|' evtype)* ('with' exp)? ';' # derivedEvtypeDecl
          | evtype NOT? 'matches' value ('with' exp)? ';' # directEvtypeDecl
          ;
object: '{' (field (',' field)*)? '}' ;
field: LOWERCASE_ID ':' value ;
value: value '|' value # orPatternVal
     | object # objectVal
     | '[' (value (',' value)*)? ellipsis? ']' # listVal
     | simpleValue # simpleVal
     ;
ellipsis: '...' ;
simpleValue: STRING # stringValue
           | INT # intValue
           | BOOLEAN # booleanValue
           | LOWERCASE_ID # varValue
           | '[' simpleValues? (',' ellipsis)? ']' # listValue
           | '_' # unusedVal
           ;
texpDecl: UPPERCASE_ID ('<' vars '>')? '=' texp ';' ;
texp: texp '*' # starTExp
    | texp '+' # plusTExp
    | texp '?' # optionalTExp
    | texp '!' # closureTExp
    | <assoc=right> texp texp # catTExp
    | texp '/\\' texp # andTExp
    | texp '\\/' texp # orTExp
    | texp '|' texp # shufTExp
    | evtype '>>' texp (':' texp)? # filterExp
    | evtype '>' texp (':' texp)? # condFilterExp
    | 'empty' # emptyTExp
    | 'none' # noneTExp
    | 'all' # allTExp
    | '{' ('var'|'let') vars ';' texp '}' # blockTExp
    | 'if' '(' exp ')' texp 'else' texp # ifElseTExp
    | UPPERCASE_ID ('<' exp (',' exp)* '>')? # varTExp
    | evtype 'with' '(' exp ')' # evtypeWithTExp
    | evtype # evtypeTExp
    | '(' texp ')' # parTExp
    ;
vars: LOWERCASE_ID (',' LOWERCASE_ID)*;
evtype: LOWERCASE_ID ('(' simpleValues? ')')? ;
simpleValues: simpleValue (',' simpleValue)* ;

exp: BOOLEAN # boolExp
   | INT # intExp
   | FLOAT # floatExp
   | LOWERCASE_ID # varExp
   | '-' exp # unaryMinusExp
   | exp '*' exp # mulExp
   | exp '/' exp # divExp
   | exp '+' exp # sumExp
   | exp '-' exp # subExp
   | exp '<' exp # lessThanExp
   | exp '<=' exp # lessThanEqExp
   | exp '>' exp # greaterThanExp
   | exp '>=' exp # greaterThanEqExp
   | exp '==' exp # equalToExp
   | exp '=' exp # assignExp
   | exp '&&' exp # andExp
   | exp '||' exp # orExp
   | '(' exp ')' # parenExp ;

// put keywords before identifiers
NOT: 'not' ;
BOOLEAN: 'false' | 'true' ;
UPPERCASE_ID: [A-Z] ID_CHAR* ;
LOWERCASE_ID: [a-z] ID_CHAR* ;
fragment ID_CHAR: [a-zA-Z0-9_] ;
INT: [0-9]+ ;
FLOAT: INT '.' INT ;
STRING: '\'' [ a-zA-Z0-0_.]* '\'' ;

WHITESPACE: [ \t\r\n]+ -> skip ;
// don't use [^\r\n]*, it's not the same
COMMENT: '//' ~[\r\n]* -> skip ;
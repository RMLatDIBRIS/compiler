grammar RML;

// generated parser will end up in this package
@header { package rml.parser; }

// a specification is made up of event type declarations followed by equations
// ignore unused warning: this is the entry point
specification: eventTypeDeclaration* equation+ EOF;

// either derive an event type from existing ones or define a eventExpression for JSON events
eventTypeDeclaration: declared=evtype NOT? MATCHES parents+=evtype ('|' parents+=evtype)* (WITH dataExp)? ';' # derivedEvtypeDecl
                    | evtype NOT? MATCHES eventExp (WITH dataExp)? ';' # directEvtypeDecl
                    ;

// event types are parametric
evtype: evtypeId ('(' evtypeParam (',' evtypeParam)* ')')? ;
evtypeParam: evtypeVar # evtypeVarParam
           | dataExp # evtypeDataExpParam
           | eventExp # evtypeEventExpParam
           ;

// patterns for JSON events from the underlying event domain
// basically JSON objects with '|'
eventExp: eventExp ('|' eventExp) # patternEventExp
        | '{' (field (',' field)*)? '}' # objectEventExp
        | listEventExp # listEventExpr // note last 'r' to avoid name clashing with rule
        | '(' eventExp ')' # parenEventExp
        | STRING # stringEventExp
        | INT # intEventExp
        | FLOAT # floatEventExp
        | BOOLEAN # boolEventExp
	| NULL # nullEventExp
        | evtypeVar # varEventExp
        | '_' # ignoredEventExp
        ;
field: fieldKey ':' eventExp ;
listEventExp: '[' ']' # emptyList
            | '[' ELLIPSIS ']' # ellipsisList
            | '[' eventExp (',' eventExp)* (',' ELLIPSIS)? ']' # nonEmptyList
            ;

// possibly generic expression
equation: expId ('<' expVar (',' expVar)* '>')? '=' exp ';' ;

// specification expression
// can't factorize unary and binary expressions easily because mutual left-recursion is not supported
exp: exp '*' # starExp
   | exp '+' # plusExp
   | exp '?' # optionalExp
   | exp '!' # closureExp
   | <assoc=right> exp exp # catExp
   | exp '/\\' exp # andExp
   | exp '\\/' exp # orExp
   | exp '|' exp # shufExp
   | evtype '>>' leftBranch=exp (':' rightBranch=exp)? # filterExp
   | EMPTY # emptyExp
   | ALL # allExp
   | '{' DEC evtypeVar (',' evtypeVar)* ';' exp '}' # blockExp
   | IF '(' dataExp ')' exp ELSE exp # ifElseExp
   | expId ('<' dataExp (',' dataExp)* '>')? # varExp
   | evtype # evtypeExp
   | '(' exp ')' # parenExp
   ;

// boolean/arithmetic expression
dataExp: BOOLEAN # boolDataExp
       | NULL # nullDataExp
       | INT # intDataExp
       | FLOAT # floatDataExp
       | evtypeVar # varDataExp
       | ABS '(' dataExp ')' # absDataExp
       | SIN '(' dataExp ')' # sinDataExp
       | COS '(' dataExp ')' # cosDataExp
       | TAN '(' dataExp ')' # tanDataExp
       | MIN '(' dataExp ',' dataExp ')' # minDataExp
       | MAX '(' dataExp ',' dataExp ')' # maxDataExp
       | dataExp '^' dataExp # expDataExp
       | '-' dataExp # minusDataExp
       | dataExp '*' dataExp # mulDataExp
       | dataExp '/' dataExp # divDataExp
       | dataExp '+' dataExp # sumDataExp
       | dataExp '-' dataExp # subDataExp
       | dataExp '<' dataExp # lessThanDataExp
       | dataExp '<=' dataExp # lessThanEqualToDataExp
       | dataExp '>' dataExp # greaterThanDataExp
       | dataExp '>=' dataExp # greaterThanEqualToDataExp
       | dataExp '==' dataExp # equalToDataExp
       | dataExp '!=' dataExp # notEqualToDataExp
       | dataExp '&&' dataExp # andDataExp
       | dataExp '||' dataExp # orDataExp
       | '(' dataExp ')' # parenDataExp
       ;

// identifier classes (don't make them lexical, they will get priority over each other)
evtypeId: LOWERCASE_ID ;
fieldKey: LOWERCASE_ID ;
evtypeVar: LOWERCASE_ID ;
expVar: LOWERCASE_ID ;
expId: UPPERCASE_ID ;

// keywords (before identifiers)
NOT: 'not' ;
MATCHES: 'matches' ;
WITH: 'with' ;
EMPTY: 'empty' ;
ALL: 'all' ;
DEC: 'var'|'let' ;
IF: 'if' ;
ELSE: 'else' ;
ABS: 'abs' ;
SIN: 'sin' ;
COS: 'cos' ;
TAN: 'tan' ;
MIN: 'min' ;
MAX: 'max' ;
BOOLEAN: 'false' | 'true' ;
NULL: 'null' ;

// identifiers
UPPERCASE_ID: [A-Z] ID_CHAR* ;
LOWERCASE_ID: [a-z] ID_CHAR* ;
fragment ID_CHAR: [a-zA-Z0-9_] ;

// other
ELLIPSIS: '...' ;
INT: [0-9]+ ;
FLOAT: INT '.' INT ;
STRING: ['] (~['\\]|'\\'[trn'\\])* ['] ;
// STRING: '\'' (~[\'\\]|'\'['\'' '\'])* '\'' ;

// things to ignore
WHITESPACE: [ \t\r\n]+ -> skip ;
COMMENT: '//' ~[\r\n]* -> skip ; // don't use [^\r\n]*, it's not the same

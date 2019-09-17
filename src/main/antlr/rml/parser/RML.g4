grammar RML;

// generated parser will end up in this package
@header { package rml.parser; }

// a specification is made up of event type declarations followed by equations
// ignore unused warning: this is the entry point
specification: eventTypeDeclaration* equation+ ;

// either derive an event type from existing ones or define a eventExpression for JSON events
eventTypeDeclaration: evtype NOT? 'matches' evtype ('|' evtype)* ('with' dataExp)? ';' # derivedEvtypeDecl
                    | evtype NOT? 'matches' eventExp ('with' dataExp)? ';' # directEvtypeDecl
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
exp: exp '*' # starExp
   | exp '+' # plusExp
   | exp '?' # optionalExp
   | exp '!' # closureExp
   | <assoc=right> exp exp # catExp
   | exp '/\\' exp # andExp
   | exp '\\/' exp # orExp
   | exp '|' exp # shufExp
   | evtype '>>' exp (':' exp)? # filterExp
   | 'empty' # emptyExp
   | 'none' # noneExp
   | 'all' # allExp
   | '{' ('var'|'let') evtypeVar (',' evtypeVar)* ';' exp '}' # blockExp
   | 'if' '(' dataExp ')' exp 'else' exp # ifElseExp
   | expId ('<' dataExp (',' dataExp)* '>')? # varExp
   | evtype # evtypeExp
   | '(' exp ')' # parenExp
   ;

// boolean/arithmetic expression
dataExp: BOOLEAN # boolDataExp
       | INT # intDataExp
       | FLOAT # floatDataExp
       | evtypeVar # varDataExp
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
BOOLEAN: 'false' | 'true' ;

// identifiers
UPPERCASE_ID: [A-Z] ID_CHAR* ;
LOWERCASE_ID: [a-z] ID_CHAR* ;
fragment ID_CHAR: [a-zA-Z0-9_] ;

// other
ELLIPSIS: '...' ;
INT: [0-9]+ ;
FLOAT: INT '.' INT ;
STRING: '\'' [ a-zA-Z0-9_.]* '\'' ;

// things to ignore
WHITESPACE: [ \t\r\n]+ -> skip ;
COMMENT: '//' ~[\r\n]* -> skip ; // don't use [^\r\n]*, it's not the same

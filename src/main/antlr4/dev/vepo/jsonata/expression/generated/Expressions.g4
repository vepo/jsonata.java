grammar Expressions;

expressions: expression+ EOF;

expressionGroup: expression+;

expression:
      DOT? fieldPath DOT? # queryPath 
    | ROOT                                 # rootPath
    | fieldPredicate                       # fieldPredicateArray
    | indexPredicate                       # indexPredicateArray 
    | rangePredicate                       # rangePredicateArray
    | arrayConstructor                     # arrayConstructorMapping
    | ARRAY_CAST                           # transformerArrayCast
    | WILDCARD WILDCARD DOT fieldName      # transformerDeepFindByField
    | WILDCARD                             # transformerWildcard
    | stringConcat                         # transformerStringConcat
    | '(' expressionGroup ')'              # innerExpression
    | booleanExpression                    # expressionBooleanSentence
    | booleanCompare                       # expressionBooleanPredicate
    | STRING                               # stringValue
    | NUMBER                               # numberValue
    ;

fieldName: IDENTIFIER |  QUOTED_VALUE;
fieldPath: fieldName (DOT fieldName)*;
fieldPredicate: '[' IDENTIFIER '=' STRING ']';
rangePredicate: '[[' NUMBER '..' NUMBER  ']]';
indexPredicate: '[' NUMBER ']';
arrayConstructor: '[' fieldPath (',' fieldPath)* ']';
stringConcat: stringOrField ('&' stringOrField)+;
stringOrField: fieldPath | STRING | NUMBER | BOOLEAN;

booleanCompare: op=('<' | '<=' | '>' | '>=' | '!=' | '=' | 'in') expressionGroup;
booleanExpression: op=('and' | 'or') expressionGroup;

// BOOLEAN_OPERATOR: ;
ARRAY_CAST: '[]';
WILDCARD: '*';

BOOLEAN: 'true' | 'false';

STRING: 
    '\'' (ESC | ~['\\])* '\''
	| '"'  (ESC | ~["\\])* '"'
	;

ROOT : '$' ;
NUMBER: '-'? [0-9]+;
IDENTIFIER: [A-Za-z_][A-Za-z_0-9]*;
QUOTED_VALUE: '`' (~'`')* '`';


fragment ESC :   '\\' (["'\\/bfnrt] | UNICODE) ;
fragment UNICODE : ([\u0080-\uFFFF] | 'u' HEX HEX HEX HEX) ;  
fragment HEX : [0-9a-fA-F] ;

DOT: '.';

// Just ignore WhiteSpaces
WS: [ \t\r\n]+ -> skip;
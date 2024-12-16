grammar Expressions;

expressions: expression+ EOF;

expressionGroup: expression+;

expression:
      DOT? fieldName (DOT fieldName)* DOT? # queryPath 
    | ROOT                                 # rootPath
    | fieldPredicate                       # fieldPredicateArray
    | indexPredicate                       # indexPredicateArray 
    | rangePredicate                       # rangePredicateArray
    | ARRAY_CAST                           # transformerArrayCast
    | WILDCARD WILDCARD DOT fieldName      # transformerDeepFindByField
    | WILDCARD                             # transformerWildcard
    | stringConcat                         # transformerStringConcat
    | '(' expressionGroup ')'              # innerExpression
    | booleanCompare                       # expressionBooleanPredicate
    | STRING                               # stringValue
    ;

fieldName: IDENTIFIER |  QUOTED_VALUE;
fieldPredicate: '[' IDENTIFIER '=' STRING ']';
rangePredicate: '[[' NUMBER '..' NUMBER  ']]';
indexPredicate: '[' NUMBER ']';
stringConcat: stringOrField ('&' stringOrField)+;
stringOrField: (fieldName (DOT fieldName)*) | STRING | NUMBER | BOOLEAN;

booleanCompare: op=('<' | '<=' | '>' | '>=' | '!=' | '=' | 'in') expressionGroup;

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
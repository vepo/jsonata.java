grammar Expressions;

expressions: expression+;

expression:
      DOT? fieldName (DOT fieldName)* # queryPath 
    | ROOT                            # rootPath
    | fieldPredicate                  # fieldPredicateArray
    | indexPredicate                  # indexPredicateArray 
    | rangePredicate                  # rangePredicateArray
    | '(' expressions ')'             # innerExpression
    ;

fieldName: IDENTIFIER |  QUOTED_VALUE;
fieldPredicate: '[' IDENTIFIER '=' STRING ']';
rangePredicate: '[[' NUMBER '..' NUMBER  ']]';
indexPredicate: '[' NUMBER ']';

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
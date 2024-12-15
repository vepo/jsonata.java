grammar Expressions;

expressions: expression+;

expression:
      DOT? fieldName (DOT fieldName)* # queryPath 
    | predicate                       # predicateArray 
    | '(' expressions ')'             # innerExpression
    ;

fieldName: IDENTIFIER |  QUOTED_VALUE;
predicate: '[' NUMBER ']';

NUMBER: '-'? [0-9]+;
IDENTIFIER: [A-Za-z_][A-Za-z_0-9]*;
QUOTED_VALUE: '`' (~'`')* '`';

DOT: '.';

// Just ignore WhiteSpaces
WS: [ \t\r\n]+ -> skip;
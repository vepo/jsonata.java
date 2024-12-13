grammar JSONataQueries;

queries: (fieldQuery | arrayQuery)+;

fieldQuery: DOT? fieldName  (DOT fieldName )*;

arrayQuery: fieldName '[' NUMBER ']';

fieldName: IDENTIFIER |  QUOTED_VALUE;

NUMBER: '-'? [0-9]+;
IDENTIFIER: [A-Za-z_][A-Za-z_0-9]*;
QUOTED_VALUE: '`' (~'`')* '`';

DOT: '.';

// Just ignore WhiteSpaces
WS: [ \t\r\n]+ -> skip;
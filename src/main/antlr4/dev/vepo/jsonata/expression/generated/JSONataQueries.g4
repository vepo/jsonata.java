grammar JSONataQueries;

queries: (fieldQuery | arrayQuery)+;

fieldQuery: IDENTIFIER  (DOT IDENTIFIER )*;

arrayQuery: IDENTIFIER '[' NUMBER ']';

NUMBER: '-'? [0-9]+;
IDENTIFIER: [A-Za-z_][A-Za-z_0-9]*;

DOT: '.';

// Just ignore WhiteSpaces
WS: [ \t\r\n]+ -> skip;
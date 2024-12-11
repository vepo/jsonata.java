grammar JSONataQueries;

queries: (fieldQuery)+;

fieldQuery: IDENTIFIER  (DOT IDENTIFIER )*;

IDENTIFIER: [A-Za-z_][A-Za-z_0-9]*;

DOT: '.';

// Just ignore WhiteSpaces
WS: [ \t\r\n]+ -> skip;
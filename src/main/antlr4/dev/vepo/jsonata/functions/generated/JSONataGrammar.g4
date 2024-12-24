grammar JSONataGrammar;

expressions: expression+ EOF;

expressionGroup: expression+;

expression:
    functionStatement                      # functionCall 
    |  DOT? fieldPath                      # queryPath 
    | ROOT                                 # rootPath
    | fieldPredicate                       # fieldPredicateArray
    | indexPredicate                       # indexPredicateArray 
    | rangePredicate                       # rangePredicateArray
    | DOT? arrayConstructor                # arrayConstructorMapping
    | ARRAY_CAST                           # transformerArrayCast
    | WILDCARD WILDCARD DOT fieldName DOT? # transformerDeepFindByField
    | (DOT WILDCARD | WILDCARD DOT)        # transformerWildcard
    | stringConcat                         # transformerStringConcat
    | DOT? '(' expressionGroup ')'         # innerExpression
    | booleanExpression                    # expressionBooleanSentence
    | booleanCompare                       # expressionBooleanPredicate
    | algebraicExpression                  # expressionAlgebraic
    | DOT objectExpression                 # objectMapper
    | objectExpression                     # objectBuilder
    | STRING                               # stringValue
    | NUMBER                               # numberValue
    ;

objectExpression:
	'{' fieldPathOrString ':' fieldPathOrString ARRAY_CAST? (
		',' fieldPathOrString ':' fieldPathOrString ARRAY_CAST?
	)* '}';
fieldPathOrString: fieldPath | STRING | NUMBER | BOOLEAN | objectExpression;
fieldName: IDENTIFIER |  QUOTED_VALUE;
fieldPath: fieldName (DOT fieldName)*;
functionStatement: IDENTIFIER '(' parameterStatement (',' parameterStatement)*  ')' ;
parameterStatement: fieldPath | functionDeclaration;
functionDeclaration: 
    'function' '(' IDENTIFIER (',' IDENTIFIER)* ')' '{' expressionGroup '}' # functionDeclarationBuilder
    ;
fieldPredicate: '[' IDENTIFIER '=' STRING ']';
rangePredicate: '[[' NUMBER '..' NUMBER  ']]';
indexPredicate: '[' NUMBER ']';
arrayConstructor: '[' fieldPath (',' fieldPath)* ']';
stringConcat: stringOrField ('&' stringOrField)+;
stringOrField: fieldPath | STRING | NUMBER | BOOLEAN;

booleanCompare: op=('<' | '<=' | '>' | '>=' | '!=' | '=' | 'in') expressionGroup;
booleanExpression: op=('and' | 'or') expressionGroup;

algebraicExpression: op=('+' | '-' | '*' | '/' | '%' | '^') expressionGroup;

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
IDENTIFIER: [$A-Za-z_][$A-Za-z_0-9]*;
QUOTED_VALUE: '`' (~'`')* '`';


fragment ESC :   '\\' (["'\\/bfnrt] | UNICODE) ;
fragment UNICODE : ([\u0080-\uFFFF] | 'u' HEX HEX HEX HEX) ;  
fragment HEX : [0-9a-fA-F] ;

DOT: '.';

// Just ignore WhiteSpaces
WS: [ \t\r\n]+ -> skip;
grammar JSONataGrammar;

expressions: expression+ EOF;

// expressionGroup: expression+;

expression:
      IDENTIFIER                                                              # identifier 
    | '*'                                                                     # fieldValues
    | DESCEND                                                                 # allDescendantSearch
    | DOLLAR                                                                  # contextReferece
    | ROOT                                                                    # root_path
    | expression '.' expression                                               # path
    | expression ARR_OPEN ARR_CLOSE                                           # toArray
    | expression ARR_OPEN NUMBER ARR_CLOSE                                    # arrayIndexQuery
    | expression ARR_OPEN expression ARR_CLOSE                                # arrayQuery
    | expression rangePredicate                                               # rangeQuery
    | expression op=('<' | '<=' | '>' | '>=' | '!=' | '=' | 'in') expression  # booleanCompare
    | '(' expression ')'                                                      # contextValue
    | STRING                                                                  # stringValue
    | NUMBER                                                                  # numberValue
    | FLOAT                                                                   # floatValue
    | EXP_NUMBER                                                              # expNumberValue
    // functionStatement                             # functionCall
    // | DOT? fieldPath                              # queryPath
    // | ROOT                                        # rootPath
    // | fieldPredicate                              # fieldPredicateArray
    // | indexPredicate                              # indexPredicateArray
    // | rangePredicate                              # rangePredicateArray
    // | DOT? arrayConstructor                       # arrayConstructorMapping
    // | ARRAY_CAST                                  # transformerArrayCast
    // | WILDCARD WILDCARD DOT fieldName DOT?        # transformerDeepFindByField
    // | (DOT WILDCARD | WILDCARD DOT)               # transformerWildcard
    // | stringConcat                                # transformerStringConcat
    // | DOT? '(' expressionGroup ')'                # innerExpression
    // | booleanExpression                           # expressionBooleanSentence
    // | booleanCompare                              # expressionBooleanPredicate
    // | algebraicExpression                         # expressionAlgebraic
    // | DOT objectExpression                        # objectMapper
    // | objectExpression                            # objectBuilder
    // | expression '?' expression (':' expression)? # conditional
    ;


rangePredicate: ARR_OPEN ARR_OPEN NUMBER '..' NUMBER  ARR_CLOSE ARR_CLOSE;

IDENTIFIER: [\p{L}_] [\p{L}0-9_]*
	        | BACK_QUOTE ~[`]* BACK_QUOTE;
ROOT : '$$' ;
DOLLAR: '$';
DESCEND: '**';
ARR_OPEN: '[';
ARR_CLOSE: ']';

fragment BACK_QUOTE : '`';

// objectExpression:
// 	'{' fieldPathOrString ':' fieldPathOrString ARRAY_CAST? (
// 		',' fieldPathOrString ':' fieldPathOrString ARRAY_CAST?
// 	)* '}';
// fieldPathOrString: fieldPath | STRING | NUMBER | FLOAT | EXP_NUMBER | BOOLEAN | objectExpression | ;
// fieldName: IDENTIFIER |  QUOTED_VALUE;
// fieldPath: fieldName (DOT fieldName)*;
// functionStatement: IDENTIFIER '(' parameterStatement (',' parameterStatement)*  ')' ;
// parameterStatement: fieldPath | functionDeclaration;
// functionDeclaration:
//     'function' '(' IDENTIFIER (',' IDENTIFIER)* ')' '{' expressionGroup '}' # functionDeclarationBuilder
//     ;
// fieldPredicate: '[' IDENTIFIER '=' STRING ']';
// indexPredicate: '[' NUMBER ']';
// arrayConstructor: '[' fieldPath (',' fieldPath)* ']';
// stringConcat: stringOrField ('&' stringOrField)+;
// stringOrField: fieldPath | STRING | NUMBER | BOOLEAN;

// booleanCompare: op=('<' | '<=' | '>' | '>=' | '!=' | '=' | 'in') expressionGroup;
// booleanExpression: op=('and' | 'or') expressionGroup;

// algebraicExpression: op=('+' | '-' | '*' | '/' | '%' | '^') expressionGroup;

// // BOOLEAN_OPERATOR: ;
// ARRAY_CAST: '[]';
// WILDCARD: '*';

// BOOLEAN: 'true' | 'false';

STRING:
    '\'' (ESC | ~['\\])* '\''
	| '"'  (ESC | ~["\\])* '"'
	;

NUMBER: '-'? [0-9]+;
FLOAT: '-'? [0-9]+ '.' [0-9]+;
EXP_NUMBER: '-'? [0-9]+ [eE] '-'? [0-9]+;
// IDENTIFIER: [$A-Za-z_][$A-Za-z_0-9]*;
// QUOTED_VALUE: '`' (~'`')* '`';


fragment ESC :   '\\' (["'\\/bfnrt] | UNICODE);
fragment UNICODE : ([\u0080-\uFFFF] | 'u' HEX HEX HEX HEX);
fragment HEX : [0-9a-fA-F];

DOT: '.';

// Just ignore WhiteSpaces
WS: [ \t\r\n]+ -> skip;
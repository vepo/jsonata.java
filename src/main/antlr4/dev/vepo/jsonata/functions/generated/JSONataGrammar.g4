grammar JSONataGrammar;

expressions: (expression+ | object) EOF;

object: OBJ_OPEN fieldList OBJ_CLOSE                                            # objectBuilder
    ;

expression:
    functionStatement                                                         # functionCall 
    | ROOT                                                                    # rootPath
    | IDENTIFIER                                                              # identifier 
    | '*'                                                                     # fieldValues
    | DESCEND                                                                 # allDescendantSearch
    | DOLLAR                                                                  # contextReferece
    | ARR_OPEN expressionList ARR_CLOSE                                       # arrayConstructor
    | expression DOT OBJ_OPEN fieldList OBJ_CLOSE                             # objectMapper
    | expression OBJ_OPEN fieldList OBJ_CLOSE                                 # objectConstructor
    | expression DOT expression                                               # path
    | expression ARR_OPEN ARR_CLOSE                                           # toArray
    | expression ARR_OPEN NUMBER ARR_CLOSE                                    # arrayIndexQuery
    | expression ARR_OPEN expression ARR_CLOSE                                # arrayQuery
    | expression rangePredicate                                               # rangeQuery
    | expression op=('<' | '<=' | '>' | '>=' | '!=' | '=' | 'in') expression  # booleanCompare
    | expression op=('and' | 'or') expression                                 # booleanExpression
    | expression op=('+' | '-' | '*' | '/' | '%' | '^') expression            # algebraicExpression
    | expression '?' expression (':' expression)?                             # inlineIfExpression 
    | expression '&' expression                                               # concatValues
    | '(' expression ')'                                                      # contextValue
    | STRING                                                                  # stringValue
    | NUMBER                                                                  # numberValue
    | FLOAT                                                                   # floatValue
    | EXP_NUMBER                                                              # expNumberValue
    | BOOLEAN                                                                 # booleanValue    
    ;

functionStatement: IDENTIFIER '(' parameterStatement (',' parameterStatement)*  ')' ;
parameterStatement: expression | functionDeclaration;
functionDeclaration: 
    'function' '(' IDENTIFIER (',' IDENTIFIER)* ')' '{' expression+ '}' # functionDeclarationBuilder
    ;
expressionList: expression (',' expression)*;
fieldList: expression ':' expOrObject  (',' expression ':' expOrObject)*;
expOrObject: expression | object;

rangePredicate: ARR_OPEN ARR_OPEN NUMBER '..' NUMBER  ARR_CLOSE ARR_CLOSE;
BOOLEAN: 'true' | 'false';
ROOT : '$$' ;
DOLLAR: '$';
DESCEND: '**';
ARR_OPEN: '[';
ARR_CLOSE: ']';
OBJ_OPEN: '{';
OBJ_CLOSE: '}';

IDENTIFIER: [\p{L}_$] [\p{L}0-9_$]*
	        | BACK_QUOTE ~[`]* BACK_QUOTE;
fragment BACK_QUOTE : '`';

STRING:
    '\'' (ESC | ~['\\])* '\''
	| '"'  (ESC | ~["\\])* '"'
	;

NUMBER: '-'? [0-9]+;
FLOAT: '-'? [0-9]+ '.' [0-9]+;
EXP_NUMBER: '-'? [0-9]+ [eE] '-'? [0-9]+;


fragment ESC :   '\\' (["'\\/bfnrt] | UNICODE);
fragment UNICODE : ([\u0080-\uFFFF] | 'u' HEX HEX HEX HEX);
fragment HEX : [0-9a-fA-F];

DOT: '.';

// Just ignore WhiteSpaces
WS: [ \t\r\n]+ -> skip;
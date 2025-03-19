grammar JSONataGrammar;

expressions: (expression+ | object) EOF;

object: OBJ_OPEN fieldList OBJ_CLOSE                                          # objectBuilder
    ;

expression:
    ROOT                                                                      # rootPath
    | IDENTIFIER                                                              # identifier
    | '*'                                                                     # fieldValues
    | DESCEND                                                                 # allDescendantSearch
    | DOLLAR                                                                  # contextReferece
    | ARR_OPEN expressionList ARR_CLOSE                                       # arrayConstructor
    | expression DOT functionStatement                                        # functionFeed
    | functionStatement                                                       # functionCall
    | expression DOT expression                                               # path
    | expression DOT OBJ_OPEN fieldList OBJ_CLOSE                             # objectMapper
    | expression OBJ_OPEN fieldList OBJ_CLOSE                                 # objectConstructor
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
    | '(' expression ';' (expression ';')+ ')'                                # blockExpression
    | FV_NAME VAR_ASSIGN (expression|functionDeclaration)                     # variableAssignment
    | FV_NAME                                                                 # variableUsage
    | REGEX                                                                   # regexValue
    | STRING                                                                  # stringValue
    | NUMBER                                                                  # numberValue
    | FLOAT                                                                   # floatValue
    | EXP_NUMBER                                                              # expNumberValue
    | BOOLEAN                                                                 # booleanValue
    ;

functionStatement: FV_NAME (('(' parameterStatement (',' parameterStatement)*  ')') | '()' ) ;
parameterStatement: expression | functionDeclaration;
functionDeclaration:
    'function' '(' FV_NAME (',' FV_NAME)* ')' '{' expression+ '}' # functionDeclarationBuilder
    ;
expressionList: expression (',' expression)*;
fieldList: expression ':' uniqueObj expOrObject  (',' expression ':' uniqueObj expOrObject)*;
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
VAR_ASSIGN : ':=' ;
uniqueObj: (DOLLAR DOT)?;

FV_NAME: DOLLAR IDENTIFIER;

IDENTIFIER: [\p{L}_] [\p{L}0-9_$]*
	        | BACK_QUOTE ~[`]* BACK_QUOTE;
fragment BACK_QUOTE : '`';

STRING:
    '\'' (ESC | ~['\\])* '\''
	| '"'  (ESC | ~["\\])* '"'
	;

REGEX:
    '/' (ESC | ~['/])* '/' ('m' | 'i' | 'g' | 'd')?
    ;    

NUMBER: '-'? [0-9]+;
FLOAT: '-'? [0-9]+ '.' [0-9]+;
EXP_NUMBER: '-'? [0-9]+ [eE] '-'? [0-9]+;


fragment ESC :   '\\' (["'\\/bfnrt] | UNICODE);
fragment UNICODE : ([\u0080-\uFFFF] | 'u' HEX HEX HEX HEX);
fragment HEX : [0-9a-fA-F];

DOT: '.';

// Just ignore WhiteSpaces
COMMENT:  '/*' .*? '*/' -> skip;      // allow comments
WS: [ \t\r\n]+ -> skip;
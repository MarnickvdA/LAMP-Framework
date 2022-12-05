grammar Toast;
program             :  module EOF;

// Based on XML syntax, useful for validating XML files based on XML schema.
// Element tags are for describing info about the element itself.
// Element attributes are for describing info about element children.

// Top-level of a file is the module, which can be a class or interface.
module              : '<module' TYPE IS STRING NAME IS STRING '>' metadata moduleHeader moduleBody '</module>';

// Metadata containing important information about the code from a language-specific point of view.
metadata            : '<meta'
                        metaFile
                        metaLocation
                      '>' '</meta>';
metaFile            : FILE_NAME IS STRING;
memberMetadata      : '<meta' metaLocation '>' '</meta>';
metaLocation        : START_LINE IS INT END_LINE IS INT; // Start and end lines included because one file can have multiple modules.

moduleHeader        : '<header'
                        ACCESS_MODIFIER IS ACCESS_TYPE
                        (MODIFIER IS MODIFIER_TYPE)?
                        (EXTENDS IS extends=STRING)?
                        (IMPLEMENTS IS implements=STRING*)? // seperated with spaces
                      '>''</header>';
moduleBody          : '<body>'
                          constructors*
                          instanceInitializer?
                          staticInitializer?
                          moduleDeclaration*
                      '</body>';
moduleDeclaration   : field | property | method | module;

constructors        : '<constructor>' constructor '</constructor>';
constructor         : parameter* statementBlock;

instanceInitializer : '<initializer>' statementBlock '</initializer>';
staticInitializer   : '<static>' statementBlock '</static>';

field               : '<field'
                        TYPE IS type=IDENTIFIER
                        NAME IS name=STRING
                        ACCESS_MODIFIER IS ACCESS_TYPE '>'
                            ('<initializer>' initializer=expression '</initializer>')?
                      '</field>';
property            : '<property'
                        TYPE IS type=IDENTIFIER
                        NAME IS name=STRING '>'
                            ('<get>' getter=returningBody '</get>')
                            ('<set>' setter=statementBlock '</set>')?
                      '</property>';
method              : '<method'
                        TYPE IS Q type=returnType Q
                        NAME IS name=STRING
                        ACCESS_MODIFIER IS ACCESS_TYPE '>'
                            parameter*
                            '<body>' statement* '</body>'
                       '</method>';
parameter           : '<parameter' TYPE IS type=IDENTIFIER NAME IS name=IDENTIFIER '>'
                        ('<default>' defaultValue=expression '</default>')?
                      '</parameter>';

statement           : expression
                    | loopStatement
                    | ifStatement
                    | switchStatement
                    | returnStatement
                    | throwStatement
                    | tryStatement
                    | breakStatement
                    | continueStatement
                    ;

condition           : '<condition>' expression '</condition>';
statementBlock      : '<block>' body=statement* '</block>';

loopStatement       : forLoop
                    | whileLoop
                    | doWhileLoop;

forLoop             : '<for>' statementBlock '</for>';
whileLoop           : '<while>'
                        condition
                        statementBlock
                      '</while>';
doWhileLoop         : '<while>'
                        condition
                        '<do>' statementBlock '</do>'
                      '</while>';

ifStatement         : '<if>'
                        condition
                        statementBlock
                        elseIfStatement*
                        elseStatement?
                      '</if>';
elseIfStatement     : '<elseif>'
                        condition
                        statementBlock
                      '</elseif>';
elseStatement       : '<else>'
                        statementBlock
                      '</else>';

switchStatement     : '<switch>' condition switchCase+ '</switch>';
switchCase          : '<case>'
                        '<item>' expression '</item>'
                        statementBlock
                      '</case>';

returnStatement     : '<return>' returnValue=expression? '</return>';
throwStatement      : '<throw>' exception=expression '</throw>';
tryStatement        : '<try>' statementBlock catchClause+ '</try>';
catchClause         : '<catch'
                        TYPE IS Q types=IDENTIFIER+ Q // You can catch multiple exceptions within one catch
                        NAME IS Q name=IDENTIFIER Q
                        '>'

                        statementBlock
                      '</catch>';

breakStatement      : '<break/>';
continueStatement   : '<continue/>';

expression          : referenceExpr
                    | literalExpr
                    | functionCallExpr
                    | conditionalExpr
                    | nullConditionalExpr
                    | unaryExpr
                    | binaryExpr
                    | lambdaExpr
                    | switchExpr
                    | ifExpr
                    ;

referenceExpr       : '<reference>' reference=IDENTIFIER '</reference>';
literalExpr         : '<literal>' literal '</literal>';
literal             : INT           #numberLiteral
                    | STRING        #stringLiteral
                    | CHAR          #charLiteral
                    | REAL          #realLiteral
                    | BOOLEAN       #booleanLiteral
                    | IDENTIFIER    #enumLiteral;

functionCallExpr    : '<functionCall>' calledFunction=expression arguments=argument* '</functionCall>';

argument            : '<value>' value=expression '</value>' | '<escapedValue/>'; // order is important, escapedValue is for values that are ignored.

binaryExpr          : '<binary>'
                        '<leftOperand>' left=expression '</leftOperand>'
                        '<operator' TYPE IS Q (binaryOp=OPERATOR_TYPE | assignOp='=' | assignBinaryOp=ASSIGN_TYPE ) Q '/>'
                        '<rightOperand>' right=expression '</rightOperand>'
                      '</binary>'
                    ;

conditionalExpr     : '<conditional>'
                        condition
                        '<onTrue>' onTrue=expression '</onTrue>'
                        '<onFalse>' onFalse=expression '</onFalse>'
                      '</conditional>';
nullConditionalExpr : '<nullcheck>'
                        '<leftOperand>' left=expression '</leftOperand>'
                        '<rightOperand>' right=expression '</rightOperand>'
                      '</nullcheck>'; // nullable?.value

unaryExpr           : '<unary>'
                        ((unaryPrefix | unaryPostfix) | (unaryPrefix unaryPostfix))
                        '<operand>' operand=expression '</operand>'
                      '</unary>';
unaryPrefix         : '<preOperator' TYPE IS Q pre=('+' | '-' | '!' | '--' | '++' ) Q '/>';
unaryPostfix        : '<postOperator' TYPE IS Q post=('--' | '++') Q '/>';

lambdaExpr          : '<lambda>'
                        parameter*
                        statementBlock
                      '</lambda>';

switchExpr          : '<switchExpr>' '</switchExpr>';  // TODO create
ifExpr              : '<ifExpr>' '</ifExpr>';          // TODO create


returningBody       : statement* returnStatement;
returnType          : genericType | IDENTIFIER;
genericType         : 'T' ('<' lowerBound=IDENTIFIER)? ('<' upperBound=IDENTIFIER)?;

ACCESS_TYPE         : '"' ('public' | 'protected' | 'private') '"';
MODIFIER_TYPE       : '"' ('static' | 'abstract' | 'final' | 'open' | 'sealed' | 'data' | 'inner') '"';
OPERATOR_TYPE       : '+' | '-'| '*'| '/'| '%'| '**'| '&&'| '||'| '=='| '!=' | '===' | '!==' | '>='| '>'| '<='| '<'| '&'| '|'| '^'| '~'| '<<'| '>>';
ASSIGN_TYPE         : '+=' | '-='| '*='| '/='| '%='| '**='| '&&='| '||=' | '&='| '|='| '^='| '~='| '<<='| '>>=';

IS                  : '=';
Q                   : '"';
FILE_NAME           : 'fileName';
EXTENDS             : 'extends';
IMPLEMENTS          : 'implements';
START_LINE          : 'startLine';
END_LINE            : 'endLine';
TYPE                : 'type';
NAME                : 'name';
ACCESS_MODIFIER     : 'accessModifier';
MODIFIER            : 'modifier';
RETURN_TYPE         : 'returnType';

INT                 :   [0-9]+;
REAL                :   [0-9]+('.'[0-9]*)?;
BOOLEAN             :   'true' | 'false';
IDENTIFIER          :   [a-zA-Z_0-9]+;
STRING              :   '"' ~'"'+ '"';
CHAR                :   '\'' ~'\'' '\'';
WS                  :   [ \t\r\n]+ -> channel(HIDDEN);

package dev.vepo.jsonata.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * Parse-time validation listener that converts ANTLR syntax errors into
 * {@link org.antlr.v4.runtime.misc.ParseCancellationException}.
 */
public class JSONataValidator extends BaseErrorListener {
    /**
     * {@inheritDoc}
     *
     * @throws org.antlr.v4.runtime.misc.ParseCancellationException always, with line and message
     */
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
    }
}

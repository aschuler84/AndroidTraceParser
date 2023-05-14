package at.mana.trace;

public class ExecutionTraceParserException extends Exception {

    public ExecutionTraceParserException() {
        super();
    }

    public ExecutionTraceParserException(String message) {
        super(message);
    }

    public ExecutionTraceParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExecutionTraceParserException(Throwable cause) {
        super(cause);
    }

    protected ExecutionTraceParserException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

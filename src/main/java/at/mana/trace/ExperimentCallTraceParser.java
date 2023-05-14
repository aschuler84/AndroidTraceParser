package at.mana.trace;


public interface ExperimentCallTraceParser<T, S> {

    T parseExecutionTrace(T rootNode, S opt ) throws ExecutionTraceParserException;

}

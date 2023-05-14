package at.mana.trace;

public interface CallTraceVisitor<R,N,P> {

    default void initialize() {  };

    void enterMethod( R root, N node, P param );

    void exitMethod(  R root, N node, P param );

    R normalizeCallStacks( R root );

    void computeApiWeights( N node );
}

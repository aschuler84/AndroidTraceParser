package at.mana.trace;

import at.mana.trace.domain.MethodTrace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MethodTraceProvider {

    private final Map<String, Deque<MethodTrace>> threadTraceQueueCache;
    private static final Logger logger = LoggerFactory.getLogger(MethodTraceProvider.class);

    public static MethodTraceProvider from( List<MethodTrace> fullTraceRecord )  {
        return new MethodTraceProvider( fullTraceRecord
                .stream().collect(Collectors.groupingBy(MethodTrace::getThreadName))
                .entrySet().stream().collect( Collectors.toMap( Map.Entry::getKey, e -> new ArrayDeque(e.getValue() )  ) ) );
    }

    public static MethodTraceProvider empty(  )  {
        return new MethodTraceProvider( null );
    }

    private MethodTraceProvider(Map<String, Deque<MethodTrace>> threadTraceQueueCache ) {
        this.threadTraceQueueCache = threadTraceQueueCache;
    }

    public ExperimentAndroidCallTraceParser.MethodTraceProxy dequeue(String threadName, String methodId, String[] methodName ) throws IllegalArgumentException {
        // dequeues elements from the cache - ensure cache is copied before use.
        if( this.threadTraceQueueCache == null || this.threadTraceQueueCache.isEmpty() ) {
            MethodTrace m = new MethodTrace();
            m.setThreadName( threadName );
            m.setMethodId( methodId );
            return ExperimentAndroidCallTraceParser.MethodTraceProxy.from(m, false);
        }

        threadTraceQueueCache.computeIfAbsent( threadName, s -> new ArrayDeque<>() );

        if( threadTraceQueueCache.get( threadName ).isEmpty() ) {
            // skips the current sample
            return null;
        }

        MethodTrace trace = threadTraceQueueCache.get( threadName ).pop();
        if( trace == null )
            throw new IllegalArgumentException( "Could not find MethodTrace in Thread " + threadName + " with id " + methodId);
        else if( !trace.getMethodId().equals( methodId )  // possible situation, where MethodIds are altered between executions (not a common case)
                && !(trace.getClassName().equals( methodName[0] )
                && trace.getMethod().equals( methodName[1] )
                && trace.getReturnType().equals( methodName[2] ) ) ) {
            logger.warn( "Invalid MethodTrace in Thread " + threadName + " with id " + methodId);
            return null;
        }
        return ExperimentAndroidCallTraceParser.MethodTraceProxy.from(trace, true);
    }


}

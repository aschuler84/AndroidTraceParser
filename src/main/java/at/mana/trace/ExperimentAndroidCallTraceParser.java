package at.mana.trace;

import at.mana.trace.domain.ExaminedUnitProxy;
import at.mana.util.ConsoleColors;
import at.mana.util.HashUtil;
import at.mana.util.KeyValuePair;
import at.mana.trace.domain.MethodTrace;
import at.mana.trace.domain.DataPoint;
import com.google.common.primitives.Bytes;
import javassist.bytecode.Descriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExperimentAndroidCallTraceParser implements ExperimentCallTraceParser<ExaminedUnitProxy, MethodTraceProvider> {

    private Logger logger = LoggerFactory.getLogger( this.getClass() );
    private static final byte[] SLOW = new byte[]{ 0x53, 0x4c, 0x4f, 0x57  };
    private static final int GROUP_METHOD_ID = 1;
    private static final int GROUP_METHOD_NAME = 6;
    private static final String methodRegex = ".*\\s(\\dx?(\\d|[a-f])?(\\d|[a-f])?(\\d|[a-f])?(\\d|[a-f])?)\\s(.*)";
    private static final int METHOD_START_INDEX = 2;
    private static final int METHOD_END_INDEX = 6;
    private static final int THREAD_ID_START_INDEX = 0;
    private static final int THREAD_ID_END_INDEX = 2;
    private static final int TIME_START_INDEX = 6;
    private static final int TIME_END_INDEX = 10;
    private static final int WALL_TIME_START_INDEX = 10;
    private static final int WALL_TIME_END_INDEX = 14;
    private final CallTraceVisitor<ExaminedUnitProxy, MethodTrace, BiConsumer<MethodTrace,Deque<MethodTrace>>> visitor;
    private Pattern methodIdPattern = Pattern.compile(methodRegex,  Pattern.MULTILINE);
    private Pattern TAB_PATTERN = Pattern.compile( "\\t" );
    private static final String UNKNOWN = "<unknown>";

    private static final int CLASS_NAME_IDX = 0;
    private static final int METHOD_NAME_IDX = 1;
    private static final int RETURN_TYPE_IDX = 2;
    private static final int PARAMETERS_IDX = 3;
    private static final int SOURCE_FILE_IDX = 4;
    private static final int HASH_IDX = 5;



    public ExperimentAndroidCallTraceParser(CallTraceVisitor<ExaminedUnitProxy,
            MethodTrace, BiConsumer<MethodTrace, Deque<MethodTrace>>> visitor) {
        this.visitor = visitor;
    }

    @Override
    public ExaminedUnitProxy parseExecutionTrace(ExaminedUnitProxy rootNode, MethodTraceProvider provider )
            throws ExecutionTraceParserException {
        byte[] traceFile = rootNode.getSample().getTraceFile();
        rootNode.getSample().setTraceFile( null );
        int index = Bytes.indexOf(traceFile, SLOW);
        byte[] recordData = Arrays.copyOfRange( traceFile, index, traceFile.length );

        //byte[] version = Arrays.copyOfRange(recordData,4, 6);
        //byte[] dataOffset = Arrays.copyOfRange(recordData,6, 8);
        //byte[] startDateTime = Arrays.copyOfRange(recordData,8, 16);
        //byte[] recordLength = Arrays.copyOfRange(recordData,16, 18);

        ByteBuffer versionBuffer = ByteBuffer.wrap( recordData, 4, 2 );
        versionBuffer.order( ByteOrder.LITTLE_ENDIAN );
        ByteBuffer dataOffsetBuffer = ByteBuffer.wrap( recordData, 6,8 );
        dataOffsetBuffer.order( ByteOrder.LITTLE_ENDIAN );
        ByteBuffer startDateTimeBuffer = ByteBuffer.wrap( recordData, 8,16 );
        startDateTimeBuffer.order( ByteOrder.LITTLE_ENDIAN );
        ByteBuffer recordLengthBuffer = ByteBuffer.wrap( recordData, 16,18 );
        recordLengthBuffer.order( ByteOrder.LITTLE_ENDIAN );

        int versionValue = versionBuffer.getShort();
        int dataOffsetValue = dataOffsetBuffer.getShort();
        long startDateTimeValue = startDateTimeBuffer.getLong();
        int recordLengthValue = recordLengthBuffer.getShort();

        logger.info( "Header data for trace file; version: " + versionValue
                + ", offset: " + dataOffsetValue
                + ", startDateTime: " + startDateTimeValue
                + ", recordLength: " + recordLengthValue );

        byte[] recordEntries =  Arrays.copyOfRange( recordData, dataOffsetValue, recordData.length );
        assert recordEntries.length % recordLengthValue == 0;
        Map<String,String> idNames = extractMethodAndThreadIdNames( traceFile, 0, index );
        traceFile = null;
        visitor.initialize();
        return buildCallTrace( rootNode, recordEntries, recordLengthValue, idNames, provider );
    }

    private ExaminedUnitProxy buildCallTrace(final ExaminedUnitProxy traceFile, byte[] recordEntries, int recordLengthValue, Map<String, String> idNames, MethodTraceProvider provider ) {


        long start = System.currentTimeMillis();
        int i = 0;
        int runs = 0;
        long noMethods = 0, noOfEnter = 0, noOfExit = 0;
        int groupId = 0;
        Map<String, Deque<MethodTraceProxy>> methodTraceCache = new HashMap<>();
        Map<String, String[]> parsedMethodNameCache = new HashMap<>();
        //Map<String, Integer> ids = new HashMap<>() ;
        while( i < recordEntries.length ) {
            //byte[] entry = Arrays.copyOfRange( recordEntries, i, i + recordLengthValue );
            //byte[] threadId = Arrays.copyOfRange( recordEntries, i + THREAD_ID_START_INDEX, i + THREAD_ID_END_INDEX );  // thread id value
            //byte[] methodId = Arrays.copyOfRange( recordEntries, i + METHOD_START_INDEX, i + METHOD_END_INDEX );  // method id value
            //byte[] timeDelta = Arrays.copyOfRange( recordEntries, i + TIME_START_INDEX, i + TIME_END_INDEX ); // time since start
            //byte[] wallTime = Arrays.copyOfRange( recordEntries, i + WALL_TIME_START_INDEX, i + WALL_TIME_END_INDEX ); // wall time
            int timeDeltaValue  = ByteBuffer.wrap( recordEntries, i + TIME_START_INDEX, TIME_END_INDEX - TIME_START_INDEX ).order( ByteOrder.LITTLE_ENDIAN ).getInt();
            int wallTimeDeltaValue  = ByteBuffer.wrap( recordEntries, i + WALL_TIME_START_INDEX, WALL_TIME_END_INDEX - WALL_TIME_START_INDEX ).order( ByteOrder.LITTLE_ENDIAN ).getInt();
            int threadIdValue = ByteBuffer.wrap(  recordEntries, i + THREAD_ID_START_INDEX, THREAD_ID_END_INDEX - THREAD_ID_START_INDEX ).order( ByteOrder.LITTLE_ENDIAN ).getShort();

            KeyValuePair<String, Boolean> methodNameId = transform( recordEntries, i + METHOD_START_INDEX, METHOD_END_INDEX - METHOD_START_INDEX );
            if( methodNameId.getValue() ) noOfEnter++; else noOfExit++;
            noMethods = idNames.size();
            //ids.computeIfAbsent( methodNameId.getKey(), s ->  0 );
            //ids.put( methodNameId.getKey(), ids.get( methodNameId.getKey() ) + 1 );
            String methodName = idNames.get( methodNameId.getKey() );
            String methodId = methodNameId.getKey();
            if( methodName == null ) {
                // This happens, if the trace contains a method that is not listed in the method list
                logger.warn( "Could not find a method name for method id: " + methodNameId.getKey() );
                methodName = UNKNOWN;
            }
            methodNameId.setKey( threadIdValue + ":" + methodNameId.getKey() );
            if( methodNameId.getValue() ) {
                parseMethodAndAddToCache( methodId, methodName, parsedMethodNameCache );
                MethodTraceProxy node = provider.dequeue( idNames.get( threadIdValue + "" ), methodId, parsedMethodNameCache.get( methodId ) );
                if( node == null ) {
                    i += recordLengthValue;
                    runs++;
                    continue;
                }
                node.getMethodTrace().setGroup( groupId++ );
                DataPoint sample = new DataPoint();
                node.getMethodTrace().getSamples().push( sample );
                // Collecting all Datapoints for one sample
                traceFile.getSample().getDataPoints().add( sample );
                describeMethodSignature( methodId, methodName, node.getMethodTrace(), sample, parsedMethodNameCache );

                sample.setStartTimeRelative((long) timeDeltaValue);
                sample.setWallTimeUSec((long) wallTimeDeltaValue);
                sample.setThreadId(threadIdValue);
                sample.setThreadName(idNames.get(threadIdValue + ""));
                sample.setMethod( node.getMethodTrace().getMethod() );
                sample.setClassName( node.getMethodTrace().getClassName() );

                node.getMethodTrace().setTraceId( methodNameId.getKey() );
                node.getMethodTrace().setThreadId( threadIdValue + "" );
                Deque<MethodTraceProxy> stack = methodTraceCache.computeIfAbsent( methodNameId.getKey(), val -> new ArrayDeque<>());
                stack.push( node );

                if( !node.isResolved() ) {
                    // Adds all children to root trace - enable this for proper multi-sample processing.
                    traceFile.getRawMethodTraces().add(node.getMethodTrace());
                    visitor.enterMethod(traceFile, node.getMethodTrace(), (n,s) -> {
                        if (!s.isEmpty()) {
                            MethodTrace pa = s.peek();
                            pa.getChildren().add(n);
                            n.setParent(pa);
                            n.setDepth(n.getParent().getDepth() + 1);
                            visitor.computeApiWeights(n);
                            traceFile.setDepth( Math.max( traceFile.getDepth(), n.getDepth() ) );
                            // can happen with lambdas and interfaces as callback params -> eg. listener
                            if (n.getParent().isApi() && !n.isApi()) {
                                logger.debug("Found a call from an API method to a non API method: " + n.getParent() + " to " + n);
                            }
                        } else {
                            traceFile.getTrace().setDepth(0);
                            // this is a node that has to be added to the root
                            n.setParent(traceFile.getTrace());
                            traceFile.getTrace().getChildren().add( n );
                            // compute java api call frequency
                            n.setDepth(n.getParent().getDepth() + 1);
                            visitor.computeApiWeights(n);
                            traceFile.setDepth( Math.max( traceFile.getDepth(), n.getDepth() ) );
                            traceFile.getStacks().computeIfAbsent(n.getThreadName(), threadId -> new ArrayDeque<>()).add(n);
                        }
                    });
                }  else  {
                    visitor.enterMethod(traceFile, node.getMethodTrace(), (n,s)->{});
                    logger.debug( "MethodTrace with methodId " + node.getMethodTrace().getMethodId() + " already resolved, processing structure (enter) omitted" );
                }
            } else {
                // threadIdValue required by exit
                // let
                Deque<MethodTraceProxy> stack = methodTraceCache.get( methodNameId.getKey() );
                if( stack != null && !stack.isEmpty() ) {
                    MethodTraceProxy node = stack.pop();
                    if (node != null) {
                        DataPoint topSample = node.getMethodTrace().getSamples().peek();
                        if (topSample != null) {
                            topSample.setDurationMicrosecondsRaw(timeDeltaValue - topSample.getStartTimeRelative());
                            topSample.setEndTimeRelative((long) timeDeltaValue);
                            logger.debug("Time Delta computation: " + node.getMethodTrace().getMethod() + ":" + (timeDeltaValue - topSample.getDurationMicrosecondsRaw()));
                        }
                        visitor.exitMethod(traceFile, node.getMethodTrace(), (n,s) -> { });
                    }
                }
            }
            logger.debug( "Method trace result: " + methodName + ( methodNameId.getValue() ? " <enter> " : " <exit> " ) );
            i += recordLengthValue;
            runs++;
        }
        // Implement clearing of unfinished stacks
        // iterate through each stack for all threads
        // and account for situations method begin, end missing
        visitor.normalizeCallStacks( traceFile );

        logger.debug( "Trace Statistics: \nNumber of Methods: "
                + noMethods +
                //" \nNumber of Calls: " + root.getChildren().size() +
                " \nNumber of Runs: "+ runs +
                " \nNumber of Method Enter: "+ noOfEnter +
                " \nNumber of Method Exit: "+ noOfExit );
                //" \nNumber of Method Ids processed: "+ ids.values().stream().reduce(Integer::sum).get() );
        long duration = System.currentTimeMillis() - start;
        logger.info( "Duration of trace parsing: " + ConsoleColors.GREEN + new SimpleDateFormat( "mm:ss:SSS" ).format( new Date( duration ) ) + ConsoleColors.RESET );
        logger.info( "Maximum depth in call-trace: " + traceFile.getDepth() );
        return traceFile;
    }

    private void parseMethodAndAddToCache(String methodId, String methodName, Map<String,String[]> methodNameCache ) {
        if( methodName !=  null && !methodName.isEmpty() ) {
            if( methodName.equals( UNKNOWN ) ) {
                methodNameCache.computeIfAbsent(methodId, s -> {
                return new String[]{
                        UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN, UNKNOWN
                };});
            }
            String[] name = methodNameCache.computeIfAbsent(methodId, s -> {
                String[] splittedMethodName = TAB_PATTERN.split(methodName);  // creats a new string every time although method name is stored only once
                String className = splittedMethodName[0];
                String method = splittedMethodName[1];
                String signature = splittedMethodName[2];
                String fileName = splittedMethodName[3];

                String hash = HashUtil.hash( method, className, signature );

                int index = signature.indexOf(')');
                String returnType = Descriptor.toClassName(signature.substring(index + 1));
                String parameters = signature.substring(1, index);
                List<String> parArray = new ArrayList<>();
                if (parameters.length() == 1) {
                    parArray.add(Descriptor.toClassName(parameters));
                } else if (parameters.length() > 1) {
                    // check whether the string starts with one of the characters
                    while (parameters.length() >= 1 && isDescriptor(parameters.charAt(0))) {
                        //take first character and substring rest
                        boolean isArray = false;
                        if (parameters.charAt(0) == '[') {
                            isArray = true;
                            parameters = parameters.substring(1);
                        }
                        if (parameters.charAt(0) == 'L') {
                            int endIdx = parameters.indexOf(';');
                            parArray.add(Descriptor.toClassName(parameters.substring(0, endIdx + 1)) + (isArray ? "[]" : ""));
                            parameters = parameters.substring(endIdx + 1);
                        } else {
                            boolean isMatrix = false;
                            if( parameters.charAt(0) == '[' ) {
                                isMatrix = true;
                                parameters = parameters.substring(1);
                            }
                            if (parameters.charAt(0) == 'L') {
                                int endIdx = parameters.indexOf(';');
                                parArray.add(Descriptor.toClassName(parameters.substring(0, endIdx + 1)) + (isArray ? "[]" : "") + (isMatrix ? "[]" : ""));
                                parameters = parameters.substring(endIdx + 1);
                            } else {
                                parArray.add(Descriptor.toClassName(parameters.charAt(0) + "") + (isArray ? "[]" : "") + (isMatrix ? "[]" : ""));
                                parameters = parameters.substring(1);
                            }
                        }
                    }
                }
                return new String[]{
                        className, method, returnType, String.join(",", parameters), fileName, hash
                };
            });
        }
    }

    private void describeMethodSignature( String methodId, String methodName, MethodTrace methodTrace, DataPoint sample, Map<String,String[]> methodNameCache ) {
        if( methodName !=  null && !methodName.isEmpty() ) {
            String[] name =  methodNameCache.get(methodId);
            methodTrace.setClassName( name[CLASS_NAME_IDX] );
            methodTrace.setMethod( name[METHOD_NAME_IDX] );
            methodTrace.setReturnType( name[RETURN_TYPE_IDX] );
            methodTrace.setParameters( name[PARAMETERS_IDX]);
            methodTrace.setSourceFilename( name[SOURCE_FILE_IDX] );
            methodTrace.setHash(name[HASH_IDX]);
            if( sample != null ) {  // Set data also in sample
                sample.setClassName( name[CLASS_NAME_IDX] );
                sample.setMethod( name[METHOD_NAME_IDX] );
                sample.setReturnType( name[RETURN_TYPE_IDX] );
                sample.setParameters( name[PARAMETERS_IDX] );
                sample.setJavaFilename( name[SOURCE_FILE_IDX] );
                sample.setHash( name[HASH_IDX] );
            }
        }
    }

    private Map<String, String> extractMethodAndThreadIdNames(byte[] copyOfRange, int offset, int length) {
        Matcher matchMethodId = methodIdPattern.matcher(new String( copyOfRange, offset, length ) );
        Map<String, String> methodIdNameMap = new HashMap<>();
        while(matchMethodId.find()) {
            methodIdNameMap.put(matchMethodId.group(GROUP_METHOD_ID), matchMethodId.group(GROUP_METHOD_NAME));
        }
        return methodIdNameMap;
    }

    private KeyValuePair<String, Boolean> transform(byte[] data, int offset, int length) {
        ByteBuffer dataBuffer = ByteBuffer.wrap( data, offset, length ).order(ByteOrder.LITTLE_ENDIAN);
        int mint = dataBuffer.getInt();
        int woMethodId = mint & 0xFFFFFFFC;

        String hex = Integer.toHexString( woMethodId );
        if( hex.equals( "0" ) )
            return new KeyValuePair<>( hex,  (mint & 0x3) == 0 );
        else if( hex.length() > 2 ) {
            return new KeyValuePair<>( "0x" + hex.trim(), (mint & 0x3) == 0 );
        } else {
            return new KeyValuePair<>( "0x" + hex, (mint & 0x3) == 0 );
        }
    }

    private boolean isDescriptor(char s) {
        return  s == '[' || s == 'V' || s == 'I' || s == 'B' ||
                s == 'J' || s == 'D' || s == 'F' || s == 'C' ||
                s == 'S' || s == 'Z' || s == 'L';
    }

    static class MethodTraceProxy {
        private MethodTrace methodTrace;
        private boolean resolved;

         private MethodTraceProxy( MethodTrace trace, boolean resolved ) {
            this.methodTrace = trace;
            this.resolved = resolved;
        }

        static MethodTraceProxy from( MethodTrace trace, boolean resolved ) {
            return new MethodTraceProxy(trace, resolved);
        }

        public MethodTrace getMethodTrace() {
            return methodTrace;
        }

        public boolean isResolved() {
            return resolved;
        }

    }



}

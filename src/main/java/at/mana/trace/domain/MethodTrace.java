package at.mana.trace.domain;


import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class MethodTrace {

    public interface NodeVisitor {
        void nodeVisited(MethodTrace node);
    }

    private Long id;


    private MethodTrace parent;

    private List<MethodTrace> children = new ArrayList<>();

    private Deque<DataPoint> samples = new ArrayDeque<>();


    private String methodId;
    private String traceId;
    private Integer group;
    private String threadId;
    private String threadName;
    private String method;
    private String className;
    private String returnType;
    private String parameters;
    private String sourceFilename;
    private String hash;
    private int depth;
    private int sampleCount;
    private boolean api;

    private double ampsAvg;
    private double ampsStd;
    private double ampsCount;
    private double ampsMedian;

    private double voltsAvg;
    private double voltsStd;
    private double voltsCount;
    private double voltsMedian;

    private double wattsAvg;
    private double wattsStd;
    private double wattsCount;
    private double wattsMedian;

    private double energyAvg;
    private double energyStd;
    private double energyCount;
    private double energyMedian;
    private double energyMin;
    private double energyMax;
    private double energyQ1;
    private double energyQ3;

    private double durationAvg;
    private double durationStd;
    private double durationCount;
    private double durationMedian;

    private double confidence;

    public Map<String, Double> javaApiStatistics = new HashMap<>();

    public Map<String, Double> javaApiStatisticsInclusive = new HashMap<>();

    public Map<String, Double> javaApiStatisticsInclusiveWeighted = new HashMap<>();

    public void addJavaTraceFrequencyStatistics( String pack ) {
        if( pack != null && !pack.isEmpty() ) {
            this.javaApiStatistics.computeIfAbsent( pack, s -> 0.0);
            // exponential decay


            this.javaApiStatistics.put( pack, this.javaApiStatistics.get( pack ) + 1  );
            this.javaApiStatisticsInclusive.computeIfAbsent( pack, s -> 0.0);
            this.javaApiStatisticsInclusive.put( pack, this.javaApiStatisticsInclusive.get( pack ) + 1 );
        }
    }

    public Double getAmpsStdErr() {
        return ampsStd / Math.sqrt( ampsCount );
    }

    public Double getAmpsRelStdErr() {
        return getAmpsStdErr() / ampsAvg;
    }

    public Double getAmpsCov() {
        return ampsStd / ampsAvg;
    }

    public Double getVoltsStdErr() {
        return voltsStd / Math.sqrt( voltsCount );
    }

    public Double getVoltsRelStdErr() {
        return getVoltsStdErr() / voltsAvg;
    }

    public Double getVoltsCov() {
        return voltsStd / voltsAvg;
    }

    public Double getWattsStdErr() {
        return wattsStd / Math.sqrt( wattsCount );
    }

    public Double getWattsRelStdErr() {
        return getWattsStdErr() / wattsAvg;
    }

    public Double getWattsCov() {
        return wattsStd / wattsAvg;
    }

    public Double getEnergyStdErr() {
        return energyStd / Math.sqrt( energyCount );
    }

    public Double getEnergyRelStdErr() {
        return getEnergyStdErr() / energyAvg;
    }

    public Double getEnergyCov() {
        return energyStd / energyAvg;
    }

    public Double getDurationStdErr() {
        return durationStd / Math.sqrt( durationCount );
    }

    public Double getDurationRelStdErr() {
        return getDurationStdErr() / durationAvg;
    }

    public Double getDurationCov() {
        return durationStd / durationAvg;
    }



    public void visitNode( NodeVisitor nodeVisitor ) {
        nodeVisitor.nodeVisited( this );
        if( !this.children.isEmpty() ) {
            for (var element : this.children ) {
                element.visitNode( nodeVisitor );
            }
            //this.children.forEach( element -> element.visitNode( nodeVisitor ) );
        }
    }

    /*private String traceId;
    private String methodId;
    private Long time;
    private String method;  // method is the name of the executed method including className (runtime)
    private String className;
    private String returnType;
    private String[] parameters;
    private String javaFilename;

    private Deque<TraceSample> samples = new ArrayDeque<>();

    private Long startTime;  // start of the trace as microseconds since epoch
    private Long endTime;  // end of the trace as microseconds since epoch
    private Long samplingStart;
    private Long durationMicrosecondsRaw; // microseconds

    private Long startTimeRelative;
    private Long endTimeRelative;
    private Long wallTimeUSec;

    private Double ampere;
    private Double voltage;
    private Double watts;
    private Double energyConsumption;

    private int startIndexOfData;
    private int endIndexOfData;
    private Double duration;
    private Double durationAdjusted;

    private Long sampleNumber;

    @Properties(prefix = "stat", allowCast = true)
    @JsonIgnore
    public Map<String, Double> javaApiStatistics = new HashMap<>();

    @Properties(prefix = "stat.incl", allowCast = true)
    @JsonIgnore
    public Map<String, Double> javaApiStatisticsInclusive = new HashMap<>();

    @Properties(prefix = "stat.incl.weighted", allowCast = true)
    @JsonIgnore
    public Map<String, Double> javaApiStatisticsInclusiveWeighted = new HashMap<>();

    private boolean apiMethod;
    private int depth;
    private int groupId;
    private int threadId;
    private String threadName;

    @Deprecated
    @JsonIgnore
    private Double relativeEnergyConsumption = 0.0;

    @Relationship( type = "trace", direction = Relationship.OUTGOING )
    @JsonIgnore
    private Method referencedMethod;

    @Transient
    @JsonIgnore
    private double[] timeSeries;
    @Transient
    @JsonIgnore
    private double[] ampereSeries;
    @Transient
    @JsonIgnore
    private double[] voltageSeries;

    public MethodTrace() {
    }

    @JsonAnyGetter
    public Map<String, Double> getJavaApiStatisticsInclusive() {
        return javaApiStatisticsInclusive;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getDurationMicrosecondsRaw() {
        return durationMicrosecondsRaw;
    }

    public void setDurationMicrosecondsRaw(Long durationMicrosecondsRaw) {
        this.durationMicrosecondsRaw = durationMicrosecondsRaw;
    }

    public String[] getParameters() {
        return parameters;
    }

    public void setParameters(String[] parameters) {
        this.parameters = parameters;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public Double getEnergyConsumption() {
        return energyConsumption;
    }

    public void setEnergyConsumption(Double energyConsumption) {
        this.energyConsumption = energyConsumption;
    }

    public int getStartIndexOfData() {
        return startIndexOfData;
    }

    public void setStartIndexOfData(int startIndexOfData) {
        this.startIndexOfData = startIndexOfData;
    }

    public int getEndIndexOfData() {
        return endIndexOfData;
    }

    public void setEndIndexOfData(int endIndexOfData) {
        this.endIndexOfData = endIndexOfData;
    }

    public Double getRelativeEnergyConsumption() {
        return relativeEnergyConsumption;
    }

    public void setRelativeEnergyConsumption(Double relativeEnergyConsumption) {
        this.relativeEnergyConsumption = relativeEnergyConsumption;
    }

    public ResultLine getResultLine() {
        return resultLine;
    }

    public void setResultLine(ResultLine resultLine) {
        this.resultLine = resultLine;
    }

    public Double getAmpere() {
        return ampere;
    }

    public void setAmpere(Double ampere) {
        this.ampere = ampere;
    }

    public Double getVoltage() {
        return voltage;
    }

    public void setVoltage(Double voltage) {
        this.voltage = voltage;
    }

    public Method getReferencedMethod() {
        return referencedMethod;
    }

    public void setReferencedMethod(Method referencedMethod) {
        this.referencedMethod = referencedMethod;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public Long getSamplingStart() {
        return samplingStart;
    }

    public void setSamplingStart(Long samplingStart) {
        this.samplingStart = samplingStart;
    }

    public Double getWatts() {
        return watts;
    }

    public void setWatts(Double watts) {
        this.watts = watts;
    }

    public Double getDurationAdjusted() {
        return durationAdjusted;
    }

    public void setDurationAdjusted(Double durationAdjusted) {
        this.durationAdjusted = durationAdjusted;
    }

    public double[] getTimeSeries() {
        return timeSeries;
    }

    public void setTimeSeries(double[] timeSeries) {
        this.timeSeries = timeSeries;
    }

    public double[] getAmpereSeries() {
        return ampereSeries;
    }

    public void setAmpereSeries(double[] ampereSeries) {
        this.ampereSeries = ampereSeries;
    }

    public double[] getVoltageSeries() {
        return voltageSeries;
    }

    public void setVoltageSeries(double[] voltageSeries) {
        this.voltageSeries = voltageSeries;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getJavaFilename() {
        return javaFilename;
    }

    public void setJavaFilename(String javaFilename) {
        this.javaFilename = javaFilename;
    }

    public Long getStartTimeRelative() {
        return startTimeRelative;
    }

    public void setStartTimeRelative(Long startTimeRelative) {
        this.startTimeRelative = startTimeRelative;
    }

    public Long getEndTimeRelative() {
        return endTimeRelative;
    }

    public void setEndTimeRelative(Long endTimeRelative) {
        this.endTimeRelative = endTimeRelative;
    }

    public void addJavaTraceFrequencyStatistics( String pack, double weight ) {
        if( pack != null && !pack.isEmpty() ) {
            this.javaApiStatistics.computeIfAbsent( pack, s -> 0.0);
            this.javaApiStatistics.put( pack, this.javaApiStatistics.get( pack ) + ( 1.0 / weight )  );
            this.javaApiStatisticsInclusive.computeIfAbsent( pack, s -> 0.0);
            this.javaApiStatisticsInclusive.put( pack, this.javaApiStatisticsInclusive.get( pack ) + ( 1.0 / weight ) );
        }
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setParent(MethodTrace parent) {
        this.parent = parent;
    }

    public MethodTrace getParent() {
        return parent;
    }

    public List<MethodTrace> getChildren() {
        return children;
    }

    public void setChildren(List<MethodTrace> children) {
        this.children = children;
    }

    public boolean hasChildren() {
        return !this.children.isEmpty();
    }

    public boolean isApiMethod() {
        return apiMethod;
    }

    public void setApiMethod(boolean apiMethod) {
        this.apiMethod = apiMethod;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public Long getSampleNumber() {
        return sampleNumber;
    }

    public void setSampleNumber(Long sampleNumber) {
        this.sampleNumber = sampleNumber;
    }

    public Long getWallTimeUSec() {
        return wallTimeUSec;
    }

    public void setWallTimeUSec(Long wallTimeUSec) {
        this.wallTimeUSec = wallTimeUSec;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getMethodId() {
        return methodId;
    }

    public void setMethodId(String methodId) {
        this.methodId = methodId;
    }

    public Deque<TraceSample> getSamples() {
        return samples;
    }

    public void setSamples(Deque<TraceSample> samples) {
        this.samples = samples;
    }

    public @Nullable TraceSample getLastSample() {
        return this.samples.peek(  );
    }

    public void visitNode(NodeVisitor nodeVisitor ) {
        nodeVisitor.nodeVisited( this );
        if( !this.children.isEmpty() ) {
            this.children.forEach( element -> element.visitNode( nodeVisitor ) );
        }
    }

    public void visitNodeLeafsFirst(NodeVisitor nodeVisitor ) {        
        if( !this.children.isEmpty() ) {
            this.children.forEach( element -> element.visitNode( nodeVisitor ) );
        }
        nodeVisitor.nodeVisited( this );
    }

    public String toRowString( char delimiter, List<String> apiColumnNames ) {
        final StringBuilder builder = new StringBuilder();
        builder.append( this.getTraceId() ).append(",");
        builder.append( this.getThreadId() ).append(",");
        builder.append( this.getSampleNumber() ).append(",");
        builder.append( this.getTime() ).append(",");
        builder.append( this.getMethod() ).append(",");
        builder.append( this.getClassName() ).append(",");
        builder.append( this.getReturnType() ).append(",");
        builder.append( this.parameters != null ? String.join(" ", this.getParameters()) : "").append(",");
        builder.append( this.getJavaFilename() ).append(",");
        builder.append( this.getStartTime() ).append(",");
        builder.append( this.getEndTime() ).append(",");
        builder.append( this.getSamplingStart() ).append(",");
        builder.append( this.getDurationMicrosecondsRaw() ).append(",");
        builder.append( this.getStartTimeRelative() ).append(",");
        builder.append( this.getEndTimeRelative() ).append(",");
        builder.append( this.getAmpere() ).append(",");
        builder.append( this.getVoltage() ).append(",");
        builder.append( this.getWatts() ).append(",");
        builder.append( this.getEnergyConsumption() ).append(",");
        builder.append( this.getDuration() ).append(",");
        builder.append( this.getDurationAdjusted() ).append(",");
        builder.append( this.isApiMethod() ).append(",");
        builder.append( this.getDepth() ).append(",");
        builder.append( this.getStartIndexOfData() ).append(",");
        builder.append( this.getEndIndexOfData() ).append(",");

        // map java columns
        apiColumnNames.forEach( s -> builder.append( this.javaApiStatisticsInclusive.getOrDefault( s, 0.0 ) ).append(',') );
        return builder.deleteCharAt( builder.length()-1 ).toString();
    }

    public static String toRowHeader( char delimiter, List<String> apiColumnNames ) {
        final StringBuilder builder = new StringBuilder();
        builder.append( "traceId" ).append(",");
        builder.append( "threadId" ).append(",");
        builder.append( "sampleNumber" ).append(",");
        builder.append( "time" ).append(",");
        builder.append( "method" ).append(",");
        builder.append( "className" ).append(",");
        builder.append( "returnType" ).append(",");
        builder.append( "paramters").append(",");
        builder.append( "filename" ).append(",");
        builder.append( "startTime" ).append(",");
        builder.append( "endTime" ).append(",");
        builder.append( "samplingStart" ).append(",");
        builder.append( "durationMicroSeconds" ).append(",");
        builder.append( "startTimeRelative" ).append(",");
        builder.append( "endTimeRelative" ).append(",");
        builder.append( "amps" ).append(",");
        builder.append( "voltage" ).append(",");
        builder.append( "watts" ).append(",");
        builder.append( "energyConsumption" ).append(",");
        builder.append( "duration" ).append(",");
        builder.append( "durationAdjusted" ).append(",");
        builder.append( "apiMethod" ).append(",");
        builder.append( "depth" ).append(",");
        builder.append( "startIndexOfData" ).append(",");
        builder.append( "endIndexOfData" ).append(",");
        // map java columns
        apiColumnNames.forEach( s -> builder.append( s ).append(',') );
        return builder.deleteCharAt( builder.length() - 1 ).toString();
    }

    public interface NodeVisitor {
        void nodeVisited(MethodTrace node);
    }

    @Override
    public String toString() {
        return "MethodTrace{" +
                "traceId='" + traceId + '\'' +
                ", className='" + className + '\'' +
                ", method='" + method + '\'' +
                ", returnType='" + returnType + '\'' +
                ", parameters=" + Arrays.toString(parameters) +
                ", javaFilename='" + javaFilename + '\'' +
                ", durationNanosRaw=" + durationMicrosecondsRaw +
                ", startTimeRelative=" + startTimeRelative +
                ", endTimeRelative=" + endTimeRelative +
                '}';
    }
 */
}

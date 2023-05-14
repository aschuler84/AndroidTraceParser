package at.mana.trace.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataPoint {

    private String method;  // method is the name of the executed method including className (runtime)
    private String className;
    private String returnType;
    private String parameters;
    private String javaFilename;
    private String threadName;
    private String hash;

    private boolean apiMethod;
    private int depth;
    private int groupId;
    private int threadId;

    private Long numberOfSamples;
    private Long startTime;  // start of the trace as microseconds since epoch
    private Long endTime;  // end of the trace as microseconds since epoch
    private Long samplingStart;
    private Long durationMicrosecondsRaw; // microseconds

    private Long startTimeRelative;  // microseconds resolution
    private Long endTimeRelative;
    private Long wallTimeUSec;

    private Double ampere;
    private Double voltage;
    private Double watts;
    private Double energyConsumption;

    private Double ampereMedian;
    private Double voltageMedian;
    private Double wattsMedian;
    private Double energyConsumptionMedian;

    private int startIndexOfData;
    private int endIndexOfData;
    private Double duration;
    private Double durationAdjusted;

    private Long sampleNumber;

    private double[] timeSeries;
    private double[] ampereSeries;
    private double[] voltageSeries;


}

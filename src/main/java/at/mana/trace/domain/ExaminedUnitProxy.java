package at.mana.trace.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Setter
@Getter
public class ExaminedUnitProxy {

    private ExaminedUnit unit;
    // volatile - only valid for one parse execution
    private Sample sample;
    // MethodTrace for root test method
    private MethodTrace trace;
    // full raw MethodTrace
    private List<MethodTrace> rawMethodTraces = new ArrayList<>();
    // Record of top-level stack frames - required for filter kernel computation
    private Map<String, Deque<MethodTrace>> stacks = new HashMap<>();
    // depth of call-graph
    private Long depth = 0L;
    // package name
    private String packageName;
    // Graph for HeatMap rendering
    private Map<String, String> attributedCallgraph = new  HashMap<>();
    // method summary
    private String traceSummary;
    // indicates the depth of the attributed graph being rendered
    private int renderingDepth = 2;
    // Number of samples to acquire
    private int noSamplesAcquire = 10;
    // Sampling frequency in Hz of measurement
    private int samplingFrequency = 5000;


    public ExaminedUnitProxy( ExaminedUnit unit ) {
        this.unit = unit;
    }

    // Attention this method has side effects
    public void setSample( Sample sample ) {
        if( this.sample != null && this.unit != null ) {
            this.unit.getSamples().add( this.sample );
        }
        this.sample = sample;
    }


}
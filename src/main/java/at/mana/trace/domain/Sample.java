package at.mana.trace.domain;


import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Sample {

    private Long id;
    private List<DataPoint> dataPoints = new ArrayList<>();

    private Double started;  // Milliseconds since epoch, however with microseconds precision
    private Double finished; // Milliseconds since epoch, however with microseconds precision

    private boolean invalid = false;

    private String energyTracesFile;
    private String deviceOutputFile;
    private String methodTraceFile;
    private String hostOutputFile;

    private byte[] traceFile;
    private byte[] deviceDataRaw;
    private byte[] energyTracesRaw;
    private long[] filterKernel;


}

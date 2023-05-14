package at.mana.trace.domain;

import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ExaminedUnit {

    private Long id;
    private List<Sample> samples = new ArrayList<>();

    private MethodTrace trace;


}

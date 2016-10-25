package jsprit.core.algorithm.state.destinationbase;

import org.apache.commons.lang.math.IntRange;

import java.util.List;

import jsprit.core.problem.Location;

public class DesBasUtils {

    public static IntRange createIndexRange(List<Location> aLocations) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (Location l : aLocations) {
            if (min > l.getIndex()) {
                min = l.getIndex();
            }
            if (max < l.getIndex()) {
                max = l.getIndex();
            }
        }
        IntRange res = new IntRange(min, max);
        return res;
    }
    
}

package jsprit.core.algorithm.state;

import java.util.Objects;

import jsprit.core.problem.solution.route.activity.TimeWindow;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.vehicle.Vehicle;

public class TimeWindowMissUtils {

    public static double calculateStartMissSecond(TourActivity aAct, Vehicle aVehicle) {
        return Math.max(0, aAct.getTheoreticalEarliestOperationStartTime() - aAct.getArrTime()); 
    }
    
    public static double calculateEndMissSecond(TourActivity aAct, Vehicle aVehicle) {
        if (aVehicle.getEarliestDeparture() > aAct.getTheoreticalLatestOperationStartTime()) {
            // Не учитываем опоздания если туда невозможно успеть
            return 0;
        }
        return Math.max(0, aAct.getArrTime() - aAct.getTheoreticalLatestOperationStartTime()); 
    }
    
    public static boolean isDefaultTimeWindow(TimeWindow aTimeWindow) {
        return aTimeWindow.getStart() < 0.01 && Objects.equals(Double.MAX_VALUE, aTimeWindow.getEnd());
    }
    
}

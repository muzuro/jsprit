package jsprit.core.algorithm.state.destinationbase;

import jsprit.core.problem.Location;
import jsprit.core.problem.vehicle.Vehicle;

public interface BaseServiceTimeProvider {

    /**
     * @param aVehicle vehicle
     * @param aLocation base location
     * @param aUnloadArriveTime base arrive jsprit time
     * @return
     */
    Double getBaseServiceTime(Vehicle aVehicle, Location aLocation, double aUnloadArriveTime);
    
}

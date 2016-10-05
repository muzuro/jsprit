package jsprit.core.algorithm.state.destinationbase;

import jsprit.core.problem.Location;
import jsprit.core.problem.vehicle.Vehicle;

public class DefaultBaseServiceTimeProvider implements BaseServiceTimeProvider {

    @Override
    public Double getBaseServiceTime(Vehicle aVehicle, Location aLocation, double aUnloadArriveTime) {
        return 0d;
    }

}

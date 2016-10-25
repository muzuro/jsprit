package jsprit.core.algorithm.state.destinationbase;

import org.apache.commons.lang.math.IntRange;

import java.util.List;
import java.util.Set;

import jsprit.core.problem.Location;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.vehicle.Vehicle;

/** Always returns all locations */
public class DefaultBaseLocationProvider implements BaseLocationProvider {

    private List<Location> locations;
    private IntRange intRange;

    public DefaultBaseLocationProvider(List<Location> aLocations) {
        locations = aLocations;
        intRange = DesBasUtils.createIndexRange(aLocations);
    }

    @Override
    public List<Location> getAvailableBaseLocations(Vehicle aVehicle, boolean aLastRun, int aRunNumber,
            int aLoadPercent, Set<LocationAssignment> aAssignedLocations, TourActivity aPrevBaseAct,
                TourActivity aPostBaseAct) {
        return locations;
    }

    @Override
    public List<Location> getAllLocations() {
        return locations;
    }

    @Override
    public List<Location> getNoVehicleLocations(TourActivity aPrevBaseAct, TourActivity aPostBaseAct) {
        return locations;
    }

    @Override
    public IntRange getUnloadLocationIndexRange() {
        return intRange;
    }

}
package jsprit.core.algorithm.state.destinationbase;

import java.util.List;
import java.util.Set;

import jsprit.core.problem.Location;
import jsprit.core.problem.vehicle.Vehicle;

/** Always returns all locations, unload duration always zero */
public class DefaultBaseLocationProvider implements BaseLocationProvider {

    private List<Location> locations;

    public DefaultBaseLocationProvider(List<Location> aLocations) {
        locations = aLocations;
    }
    
    @Override
    public List<Location> getAvailableBaseLocations(Vehicle aVehicle, boolean aLastRun, int aRunNumber,
            int aLoadPercent, Set<LocationAssignment> aAssignedLocations) {
        return locations;
    }

    @Override
    public List<Location> getAllLocations() {
        return locations;
    }

}
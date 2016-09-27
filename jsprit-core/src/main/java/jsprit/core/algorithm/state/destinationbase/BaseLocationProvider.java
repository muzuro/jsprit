package jsprit.core.algorithm.state.destinationbase;

import java.util.List;
import java.util.Set;

import jsprit.core.problem.Location;
import jsprit.core.problem.vehicle.Vehicle;

public interface BaseLocationProvider {
    /**
     * @param aVehicle - vehicle
     * @param aLastRun - is run last
     * @param aRunNumber - run number, starts from 0
     * @param aLoadPercent - run load percent
     * @param aAssignedLocations - route assigned location(with assignemnt count) 
     * @return avaliable locations
     */
    List<Location> getAvailableBaseLocations(Vehicle aVehicle, boolean aLastRun, int aRunNumber, int aLoadPercent,
            Set<LocationAssignment> aAssignedLocations);
    /**
     * @return all locations
     */
    List<Location> getAllLocations();
}
package jsprit.core.algorithm.state.destinationbase;

import org.apache.commons.lang.math.IntRange;

import java.util.List;
import java.util.Set;

import jsprit.core.problem.Location;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.vehicle.Vehicle;

public interface BaseLocationProvider {
    /**
     * @param aVehicle - vehicle
     * @param aLastRun - is run last
     * @param aRunNumber - run number, starts from 0
     * @param aLoadPercent - run load percent
     * @param aAssignedLocations - route assigned location(with assignemnt count) 
     * @param aPostBaseAct 
     * @param aPrevBaseAct 
     * @return avaliable locations
     */
    List<Location> getAvailableBaseLocations(Vehicle aVehicle, boolean aLastRun, int aRunNumber, int aLoadPercent,
            Set<LocationAssignment> aAssignedLocations, TourActivity aPrevBaseAct, TourActivity aPostBaseAct);
    /**
     * @return all locations
     */
    List<Location> getAllLocations();
    
    List<Location> getNoVehicleLocations(TourActivity aPrevBaseAct, TourActivity aPostBaseAct);
    
    IntRange getUnloadLocationIndexRange();
    
}
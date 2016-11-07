package jsprit.core.algorithm.state;

import org.apache.commons.lang.math.IntRange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jsprit.core.algorithm.state.destinationbase.BaseLocationProvider;
import jsprit.core.algorithm.state.destinationbase.BaseServiceTimeProvider;
import jsprit.core.algorithm.state.destinationbase.LocationAssignment;
import jsprit.core.problem.Capacity;
import jsprit.core.problem.Location;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.job.Base;
import jsprit.core.problem.job.Job;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.BaseService;
import jsprit.core.problem.solution.route.activity.DestinationService;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.vehicle.Vehicle;

public class DestinationBaseLoadChecker {

    private final static Logger logger = LogManager.getLogger();
    
    private final StateManager stateManager;

    private final Capacity defaultValue;

    private final Capacity firstRunCapacity;
    
    private final List<Base> basePool;
    
    private Set<Base> freeBases;
    
    private final Map<String, Double> unloadDuration;
    
    private final Double defaultUnloadDuration;
    
    private final Capacity empty = Capacity.Builder.newInstance().build();
    
    private Capacity[] unloadVolumes;

    private int minDailyIndex;

    private Capacity[] dailyCapacities;
    
    private BaseLocationProvider baseLocationProvider;
    
    private BaseServiceTimeProvider baseServiceTimeProvider;

    DestinationBaseLoadChecker(StateManager aStateManager, Capacity aFirstRunCapacity,
            BaseLocationProvider aBaseLocationProvider, BaseServiceTimeProvider aBaseServiceTimeProvider,
            Map<String, Double> aUnloadDurations) {
        baseLocationProvider = aBaseLocationProvider;
        baseServiceTimeProvider = aBaseServiceTimeProvider;
        defaultValue = Capacity.Builder.newInstance().build();
        stateManager = aStateManager;
        firstRunCapacity = aFirstRunCapacity;
        unloadDuration = aUnloadDurations;
        defaultUnloadDuration = unloadDuration.values().iterator().next();
        basePool = new ArrayList<>();
        freeBases = new HashSet<>();
    }
    
    public BaseLocationProvider getBaseLocationProvider() {
        return baseLocationProvider;
    }
    
    public int getLoadPercent(VehicleRoute route, int aRunNumber) {
        Capacity runLoad = stateManager.getRunState(route, aRunNumber, InternalStates.DestinationBase.RUN_LOAD,
                Capacity.class, defaultValue);
        Capacity vehicleCapacity = route.getVehicle().getType().getCapacityDimensions();
        return (runLoad.get(0) / vehicleCapacity.get(0)) * 100; 
    }
    
    public Set<LocationAssignment> getLocationAssignments(VehicleRoute route) {
        Integer runCount = stateManager.getRouteState(route, InternalStates.DestinationBase.RUN_COUNT, Integer.class);
        if (Objects.isNull(runCount)) {
            runCount = 0;
        }
        Set<LocationAssignment> result = new HashSet<>();
        IntRange ir = baseLocationProvider.getUnloadLocationIndexRange();
        int size = ir.getMaximumInteger() - ir.getMinimumInteger() + 1;
        LocationAssignment[] array = new LocationAssignment[size];
        for (int i = 0; i < runCount; i++) {
            Location runUnloadLocation = stateManager.getRunState(route, i,
                    InternalStates.DestinationBase.RUN_UNLOAD_LOCATION, Location.class, null);
            int rulIndex = runUnloadLocation.getIndex() - ir.getMinimumInteger();
            LocationAssignment locationAssignment = array[rulIndex];
            if (Objects.isNull(locationAssignment)) {
                locationAssignment = new LocationAssignment(runUnloadLocation, 1);
                array[rulIndex] = locationAssignment;
                result.add(locationAssignment);
            } else {
                locationAssignment.incrementCount();
            }
        }
        return result;
    }
    
    public boolean isLoaded(Job aJob, VehicleRoute route) {
        Integer runCount = stateManager.getRouteState(route, InternalStates.DestinationBase.RUN_COUNT, Integer.class);
        if (Objects.isNull(runCount)) {
            runCount = 0;
        }
        for (int i = 0; i < runCount; i++) {
            Capacity runLoad = stateManager.getRunState(route, i, InternalStates.DestinationBase.RUN_LOAD,
                    Capacity.class, defaultValue);
            Capacity vehicleCapacity = route.getVehicle().getType().getCapacityDimensions();
            if (i == 0 && Objects.nonNull(firstRunCapacity)) {
                vehicleCapacity = firstRunCapacity;
            }
            
            Location runUnloadLocation = stateManager.getRunState(route, i,
                    InternalStates.DestinationBase.RUN_UNLOAD_LOCATION, Location.class, null);
            boolean unloadAllow = isLocationDailyLoadable(runUnloadLocation, aJob.getSize());
            boolean vehicleAllow = vehicleCapacity.isGreaterOrEqual(Capacity.addup(aJob.getSize(), runLoad));
            if (vehicleAllow && unloadAllow) {
                return false;//at least one run not full
            }
        }
        return true;//all runs full
    }
    
    public Integer getRunCount(VehicleRoute route) {
        return stateManager.getRouteState(route, InternalStates.DestinationBase.RUN_COUNT, Integer.class);
    }
    
    public void markBaseRequired(VehicleRoute aRoute, boolean aRequired) {
        stateManager.putTypedInternalRouteState(aRoute, InternalStates.DestinationBase.BASE_REQUIRED, aRequired);
    }
    
    public boolean isBaseRequired(VehicleRoute aRoute) {
        Boolean routeState = stateManager.getRouteState(aRoute, InternalStates.DestinationBase.BASE_REQUIRED, Boolean.class);
        return !Boolean.FALSE.equals(routeState);
    }

    public List<Location> getBaseLocations(Vehicle aVehicle, boolean aLastRun, int aRunNumber, int aLoadPercent,
            Set<LocationAssignment> aAssignedLocations, TourActivity aPrevBaseAct, TourActivity aPostBaseAct) {
        return baseLocationProvider.getAvailableBaseLocations(aVehicle, aLastRun, aRunNumber, aLoadPercent,
                aAssignedLocations, aPrevBaseAct, aPostBaseAct);
    }
    
    public Double getUnloadDuration(Vehicle aVehicle, Location aLocation, double aUnloadArriveTime) {
        return baseServiceTimeProvider.getBaseServiceTime(aVehicle, aLocation, aUnloadArriveTime);
    }

    public void initBaseIndex(VehicleRoutingProblem aVrp) {
        int index = aVrp.getNuActivities();
        for (int i = 0; i < aVrp.getNuActivities(); i++) {
            basePool.add(Base.Builder.newInstance(String.format("base%s", i))
                    .setIndex(index++)
                    .build());
        }
        aVrp.setNuActivities(index);
        basePool.forEach(b->freeBases.add(Base.copyOf(b)));
    }

    public Base findBaseToInsert() {
        Iterator<Base> iterator = freeBases.iterator();
        Base next = iterator.next();
        return next;
    }
    
    public boolean takeBase(Base aBase) {
        return freeBases.remove(aBase);
    }
    
    public boolean releaseBase(Base aBase) {
        return freeBases.add(aBase);
    }

    public void refreshFreeBases(Collection<VehicleRoute> aVehicleRoutes) {
        basePool.forEach(b->freeBases.add(Base.copyOf(b)));
        for (VehicleRoute route: aVehicleRoutes) {
            route.getTourActivities().getJobs().stream().filter(j->j instanceof Base).map(j->(Base)j).forEach(freeBases::remove);
        }
    }

    public List<Location> getNoVehicleLocations(TourActivity aPrevBaseAct, TourActivity aPostBaseAct) {
        return baseLocationProvider.getNoVehicleLocations(aPrevBaseAct, aPostBaseAct);
    }

    public void initUnloadVolumes(int aMinDailyIndex, Capacity[] aDailyCapacities) {
        minDailyIndex = aMinDailyIndex;
        dailyCapacities = aDailyCapacities;
        unloadVolumes = new Capacity[aDailyCapacities.length];
        for (int i = 0; i < unloadVolumes.length; i++) {
            unloadVolumes[i] = Capacity.Builder.newInstance().build();
        }
        
    }

    /**
     * clear unload volumes and refill them from routes
     * @param aVehicleRoutes
     */
    public void refreshUnloadLocation(Collection<VehicleRoute> aVehicleRoutes) {
        clearUnloadVolumes();
        for (VehicleRoute route: aVehicleRoutes) {
            int runNum = 0;
            Capacity runVolume = empty;
            for (TourActivity ta : route.getActivities()) {
                if (ta instanceof DestinationService) {                    
                    runVolume = Capacity.addup(runVolume, ta.getSize()); 
                } else if (ta instanceof BaseService) {
                    // location unload volume state
                    addUnloadVolume(ta.getLocation(), runVolume);
                    runVolume = empty;
                    // unload location state
                    stateManager.putRunState(route, runNum++, InternalStates.DestinationBase.RUN_UNLOAD_LOCATION,
                            ta.getLocation());
                }
            }
        }
    }

    /**
     * mark current unload volume zero
     */
    public void clearUnloadVolumes() {
        for (int i = 0; i < unloadVolumes.length; i++) {
            unloadVolumes[i] = empty;
        }
    }
    
    public void addUnloadVolume(Location aLocation, Capacity aAdditionalLoad) {
        int index = findDailyVolumeIndex(aLocation);
        Capacity possibleUnloadCapacity = Capacity.addup(aAdditionalLoad,
                unloadVolumes[index]);
        unloadVolumes[index] = possibleUnloadCapacity;
    }

    private int findDailyVolumeIndex(Location location) {
        if (Objects.isNull(location)) {
            logger.error("wtf");
        }
        return location.getIndex() - minDailyIndex;
    }

    public boolean isLocationDailyLoadable(Location aLocation, Capacity aAdditionalLoad) {
        if (dailyCapacities == null) {
            return true;
        }
        Capacity possibleUnloadCapacity = Capacity.addup(aAdditionalLoad,
                unloadVolumes[findDailyVolumeIndex(aLocation)]);
        Capacity dailyCapacity = dailyCapacities[findDailyVolumeIndex(aLocation)];
        return dailyCapacity == null || possibleUnloadCapacity.isLessOrEqual(dailyCapacity);
    }
    
    public Capacity findDailyCapacity(Location aLocation) {
        return dailyCapacities[findDailyVolumeIndex(aLocation)];
    }
    
}

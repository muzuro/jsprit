package jsprit.core.algorithm.state;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jsprit.core.algorithm.state.destinationbase.BaseLocationProvider;
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

    private final StateManager stateManager;

    private final Capacity defaultValue;

    private final Capacity firstRunCapacity;
    
//    /** unlaods */
//    private final List<Location>[] baseLocations;//номер в массиве - vehicle.getIndex()
    
//    /** all vehicles unloads */
//    private final List<Location> allVehicleBaseLocations;
    
    private final List<Base> basePool;
    
    private Set<Base> freeBases;
    
    private final Map<String, Double> unloadDuration;
    
    private final Double defaultUnloadDuration;
    
    private final Capacity empty = Capacity.Builder.newInstance().build();
    
    private Capacity[] unloadVolumes;

    private int minDailyIndex;

    private Capacity[] dailyCapacities;
    
    private BaseLocationProvider baseLocationSelector;

    DestinationBaseLoadChecker(StateManager aStateManager, Capacity aFirstRunCapacity,
            BaseLocationProvider aBaseLocationSelector, Map<String, Double> aUnloadDurations) {
        baseLocationSelector = aBaseLocationSelector;
        defaultValue = Capacity.Builder.newInstance().build();
        stateManager = aStateManager;
        firstRunCapacity = aFirstRunCapacity;
        unloadDuration = aUnloadDurations;
        defaultUnloadDuration = unloadDuration.values().iterator().next();
        basePool = new ArrayList<>();
        freeBases = new HashSet<>();
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
        Map<Location, LocationAssignment> map = new HashMap<>();
        for (int i = 0; i < runCount; i++) {
            Location runUnloadLocation = stateManager.getRunState(route, i,
                    InternalStates.DestinationBase.RUN_UNLOAD_LOCATION, Location.class, null);
            
            LocationAssignment locationAssignment = map.get(runUnloadLocation);
            if (Objects.isNull(locationAssignment)) {                
                locationAssignment = new LocationAssignment(runUnloadLocation, 1);
                map.put(runUnloadLocation, locationAssignment);
            } else {
                locationAssignment.incrementCount();
            }
        }
        return new HashSet<>(map.values());
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
                return false;//хотяб один рейс не заполнен
            }
        }
        return true;//все рейсы заполненны
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
            Set<LocationAssignment> aAssignedLocations) {
        return baseLocationSelector.getAvailableBaseLocations(aVehicle, aLastRun, aRunNumber, aLoadPercent,
                aAssignedLocations);
    }
    
    public Double getUnloadDuration(Vehicle aVehicle) {
        return unloadDuration.getOrDefault(aVehicle, defaultUnloadDuration);
    }

    public void initBaseIndex(VehicleRoutingProblem aVrp) {
        int index = aVrp.getNuActivities();
        for (int i = 0; i < aVrp.getNuActivities(); i++) {
            basePool.add(Base.Builder.newInstance(String.format("base%s", i))
                    .setIndex(index++)
                    .build());
        }
        aVrp.setNuActivities(index);
        freeBases.addAll(basePool);
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

    public void refreshFreeJobs(Collection<VehicleRoute> aVehicleRoutes) {
        freeBases = new HashSet<>(basePool);
        for (VehicleRoute route: aVehicleRoutes) {
            route.getTourActivities().getJobs().stream().filter(j->j instanceof Base).map(j->(Base)j).forEach(freeBases::remove);
        }
    }

    public List<Location> getAllVehicleBaseLocations() {
        return baseLocationSelector.getAllLocations();
    }

    public void initUnloadVolumes(int aMinDailyIndex, Capacity[] aDailyCapacities) {
        minDailyIndex = aMinDailyIndex;
        dailyCapacities = aDailyCapacities;
        unloadVolumes = new Capacity[aDailyCapacities.length];
        for (int i = 0; i < unloadVolumes.length; i++) {
            unloadVolumes[i] = Capacity.Builder.newInstance().build();
        }
        
    }

    public void refreshUnloadLocation(Collection<VehicleRoute> aVehicleRoutes) {
        for (int i = 0; i < unloadVolumes.length; i++) {
            unloadVolumes[i] = empty;
        }
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
    
    public void addUnloadVolume(Location aLocation, Capacity aAdditionalLoad) {
        int index = findDailyVolumeIndex(aLocation);
        Capacity possibleUnloadCapacity = Capacity.addup(aAdditionalLoad,
                unloadVolumes[index]);
        unloadVolumes[index] = possibleUnloadCapacity;
    }

    private int findDailyVolumeIndex(Location location) {
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
    
}

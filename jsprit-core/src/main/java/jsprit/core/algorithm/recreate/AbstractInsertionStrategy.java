/*******************************************************************************
 * Copyright (C) 2014  Stefan Schroeder
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package jsprit.core.algorithm.recreate;


import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import jsprit.core.algorithm.recreate.listener.InsertionListener;
import jsprit.core.algorithm.recreate.listener.InsertionListeners;
import jsprit.core.algorithm.state.DestinationBaseLoadChecker;
import jsprit.core.algorithm.state.UpdateActivityTimes;
import jsprit.core.algorithm.state.destinationbase.LocationAssignment;
import jsprit.core.problem.Capacity;
import jsprit.core.problem.Location;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import jsprit.core.problem.driver.Driver;
import jsprit.core.problem.job.Base;
import jsprit.core.problem.job.Job;
import jsprit.core.problem.solution.route.RouteActivityVisitor;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.BaseService;
import jsprit.core.problem.solution.route.activity.DestinationService;
import jsprit.core.problem.solution.route.activity.End;
import jsprit.core.problem.solution.route.activity.Start;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.vehicle.Vehicle;
import jsprit.core.util.ActivityTimeTracker.ActivityPolicy;
import jsprit.core.util.RandomNumberGeneration;

public abstract class AbstractInsertionStrategy implements InsertionStrategy {

    protected class Insertion {

        private final VehicleRoute route;

        private final InsertionData insertionData;

        public Insertion(VehicleRoute vehicleRoute, InsertionData insertionData) {
            super();
            this.route = vehicleRoute;
            this.insertionData = insertionData;
        }

        public VehicleRoute getRoute() {
            return route;
        }

        public InsertionData getInsertionData() {
            return insertionData;
        }

    }

    private final static Logger logger = LogManager.getLogger();

    protected Random random = RandomNumberGeneration.getRandom();

    protected final static double NO_NEW_DEPARTURE_TIME_YET = -12345.12345;

    protected final static Vehicle NO_NEW_VEHICLE_YET = null;

    protected final static Driver NO_NEW_DRIVER_YET = null;

    private InsertionListeners insertionsListeners;

    private EventListeners eventListeners;

    protected VehicleRoutingProblem vrp;

    public AbstractInsertionStrategy(VehicleRoutingProblem vrp) {
        this.insertionsListeners = new InsertionListeners();
        this.vrp = vrp;
        eventListeners = new EventListeners();
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    @Override
    public Collection<Job> insertJobs(Collection<VehicleRoute> vehicleRoutes, Collection<Job> unassignedJobs) {
        insertionsListeners.informInsertionStarts(vehicleRoutes, unassignedJobs);
        List<Job> notBases = new ArrayList<Job>();
        for (Job job : unassignedJobs) {
            notBases.add(job);
        }
        
        //clear and recalculate unload locations and volumes before inserting
        vrp.getDestinationBaseLoadChecker().refreshUnloadLocation(vehicleRoutes);
        vrp.getDestinationBaseLoadChecker().refreshFreeBases(vehicleRoutes);
        Collection<Job> badJobs = insertUnassignedJobs(vehicleRoutes, notBases);
        insertionsListeners.informInsertionEndsListeners(vehicleRoutes);        
        
        //clear unload locations and volumes before optimizing bases
        vrp.getDestinationBaseLoadChecker().clearUnloadVolumes();

        vehicleRoutes.forEach(route->{
            badJobs.addAll(optimizeBases(route));
            new RouteActivityVisitor().addActivityVisitor(new UpdateActivityTimes(vrp.getTransportCosts(),
                    ActivityPolicy.AS_SOON_AS_ARRIVED)).visit(route);
        });
        
        return badJobs;
    }
    
    public Collection<Job> optimizeBases(VehicleRoute aRoute) {
        List<Job> badJobs = new ArrayList<>();
        Vehicle vehicle = aRoute.getVehicle();
        Start start = new Start(vehicle.getStartLocation(), vehicle.getEarliestDeparture(), Double.MAX_VALUE);
        End end = new End(vehicle.getEndLocation(), 0.0, vehicle.getLatestArrival());
        TourActivity prev = null;
        TourActivity current = start;
        TourActivity next = null;
        Iterator<TourActivity> iterator = aRoute.getActivities().iterator();
        current = iterator.next();
        boolean routeEnd = false;
        Capacity runLoad = Capacity.Builder.newInstance().build();
        int runNumber = 0;
        //we should calclute arrive time becouse it could be changed by best base calculation
        double currentArriveTime = current.getArrTime();
        
        //store this values if points will be deleted from route. 
        Double onDeletePrevBaseArriveTime = null;
        TourActivity onDeletePrevBase = null;
        
        List<TourActivity> badActivities = new ArrayList<>();
        List<TourActivity> runDestinations = new ArrayList<>();
        
        Map<Location, LocationAssignment> assignedMap = new HashMap<>();
        while (!routeEnd) {
            if (iterator.hasNext()) {
                next = iterator.next();
            } else {
                next = end;
                routeEnd = true;
            }
            if (current instanceof DestinationService) {
                runDestinations.add(current);
                runLoad = Capacity.addup(runLoad, current.getSize());
            } else if (current instanceof BaseService) {
                if (Objects.isNull(prev) || Objects.isNull(current)) {
                    continue;
                }
                
                Pair<Location, Double> selected = findBestLocation(aRoute, prev, next, runLoad, current,
                        runNumber++, new HashSet(assignedMap.keySet()), currentArriveTime);
                if (Objects.isNull(selected)) {
                    DestinationService prevDest = (DestinationService) prev;
                    badJobs.add(prevDest.getJob());
                    
                    badActivities.addAll(runDestinations);
                    badActivities.add(current);
                    
                    currentArriveTime = calculateArriveTime(onDeletePrevBaseArriveTime, onDeletePrevBase, next, aRoute);
                    prev = onDeletePrevBase;
                    
                    current = next;
                    runLoad = Capacity.Builder.newInstance().build();
                    continue;
                }
                runDestinations = new ArrayList<>();
                BaseService baseService = (BaseService) current;
                Base base = ((Base) baseService.getJob());
                Location selectedLocation = selected.getKey();
                base.setLocation(selectedLocation);
                base.setServiceDuration(selected.getValue());
                
                LocationAssignment locationAssignment = assignedMap.getOrDefault(selectedLocation,
                        new LocationAssignment(selectedLocation));
                locationAssignment.incrementCount();
                assignedMap.put(selectedLocation, locationAssignment);
                vrp.getDestinationBaseLoadChecker().addUnloadVolume(selectedLocation, runLoad);
                runLoad = Capacity.Builder.newInstance().build();
            }
            
            //prepare to next iteration
            if (prev instanceof BaseService) {
                onDeletePrevBaseArriveTime = currentArriveTime;
                onDeletePrevBase = prev;
            }
            currentArriveTime = calculateArriveTime(currentArriveTime, current, next, aRoute);
            prev = current;
            current = next;
        }
        for (TourActivity ba : badActivities) {
            aRoute.getTourActivities().removeActivity(ba);
        }
        return badJobs;
    }
    
    
    private double calculateArriveTime(double currentArriveTime, TourActivity current, TourActivity next,
            VehicleRoute aRoute) {
        double departureTime = currentArriveTime + current.getOperationTime();
        double transportTime = vrp.getTransportCosts().getTransportTime(current.getLocation(), next.getLocation(),
                departureTime, aRoute.getDriver(), aRoute.getVehicle());
        double nextArriveTime = departureTime + transportTime;
        
        //prepare to next iteration
        return nextArriveTime;
    }
    
    /**
     * @param aRoute
     * @param prev - before base point
     * @param next - after base point
     * @param aRunLoad - run capacity
     * @param aCurrent
     * @param aRunNumber - run number, starts from 0
     * @param aAssignedLocations 
     * @return base location and service time, null if not found any
     */
    private Pair<Location, Double> findBestLocation(VehicleRoute aRoute, TourActivity prev, TourActivity next,
            Capacity aRunLoad, TourActivity aCurrent, int aRunNumber, Set<LocationAssignment> aAssignedLocations,
                    Double aBaseArriveTime) {
        DestinationBaseLoadChecker destinationBaseLoadChecker = vrp.getDestinationBaseLoadChecker();
        VehicleRoutingTransportCosts transportCosts = vrp.getTransportCosts();
        Double min = Double.MAX_VALUE;
        Location bestBaseLocation = null;
        Double bestBaseServiceTime = null;
        boolean lastRun = next instanceof End;
        
        Capacity vehicleCapacity = aRoute.getVehicle().getType().getCapacityDimensions();
        float fillFactor = (float) aRunLoad.get(0) / vehicleCapacity.get(0);
        int runLoadPercent = (int) (fillFactor * 100);
        
        List<Location> allowedUnloadLocations = destinationBaseLoadChecker.getBaseLocations(aRoute.getVehicle(),
                lastRun, aRunNumber, runLoadPercent, aAssignedLocations, prev, next);
        for (Location possibleBaseLocation : allowedUnloadLocations) {
            double toBase = transportCosts.getTransportCost(prev.getLocation(),
                    possibleBaseLocation, prev.getEndTime(), aRoute.getDriver(), aRoute.getVehicle());
            Double baseProceedTime = destinationBaseLoadChecker.getUnloadDuration(aRoute.getVehicle(),
                    possibleBaseLocation, aBaseArriveTime);
            double baseDepartureTime = aBaseArriveTime + baseProceedTime;
            double fromBase = transportCosts.getTransportCost(possibleBaseLocation,
                    next.getLocation(), baseDepartureTime, aRoute.getDriver(), aRoute.getVehicle());
            double totalCost = toBase + fromBase;
            if (totalCost < min && destinationBaseLoadChecker.isLocationDailyLoadable(possibleBaseLocation, aRunLoad)) {
                bestBaseLocation = possibleBaseLocation;
                min = totalCost;
                bestBaseServiceTime = baseProceedTime;
            }
        }
        if (Objects.nonNull(bestBaseLocation)) {
            return Pair.create(bestBaseLocation, bestBaseServiceTime);
        } else {
            return null;
        }
    }

    public abstract Collection<Job> insertUnassignedJobs(Collection<VehicleRoute> vehicleRoutes, Collection<Job> unassignedJobs);

    protected void markRequiredRoutes(Collection<VehicleRoute> routes, List<Job> jobs) {
        Iterator<Job> unassignedJobIterator = jobs.iterator();
        if (unassignedJobIterator.hasNext()) {                
            Job minimalCapacityJob = unassignedJobIterator.next();
            while (unassignedJobIterator.hasNext()) {
                Job next = unassignedJobIterator.next();
                if (minimalCapacityJob.getSize().isGreater(next.getSize())) {
                    minimalCapacityJob = next;
                }
            }
            markRequiredRoutes(routes, minimalCapacityJob);
        }
    }
    
    protected void markRequiredRoutes(Collection<VehicleRoute> routes, Job aToInsert) {
        DestinationBaseLoadChecker destinationBaseLoadChecker = vrp.getDestinationBaseLoadChecker();
        for (VehicleRoute route : routes) {
            boolean loaded = destinationBaseLoadChecker.isLoaded(aToInsert, route);
            destinationBaseLoadChecker.markBaseRequired(route, loaded);
        }
    }
    
    @Override
    public void removeListener(InsertionListener insertionListener) {
        insertionsListeners.removeListener(insertionListener);
    }

    @Override
    public Collection<InsertionListener> getListeners() {
        return Collections.unmodifiableCollection(insertionsListeners.getListeners());
    }

    @Override
    public void addListener(InsertionListener insertionListener) {
        insertionsListeners.addListener(insertionListener);
    }

    protected abstract JobInsertionCostsCalculator getJobInsertionCostsCalculator();
    
    protected void insertJob(Job unassignedJob, InsertionData iData, VehicleRoute inRoute) {
        logger.trace("insert: [jobId={}]{}", unassignedJob.getId(), iData);
        insertionsListeners.informBeforeJobInsertion(unassignedJob, iData, inRoute);
        if (!(inRoute.getVehicle().getId().equals(iData.getSelectedVehicle().getId()))) {
            insertionsListeners.informVehicleSwitched(inRoute, inRoute.getVehicle(), iData.getSelectedVehicle());
        }
        for (Event e : iData.getEvents()) {
            eventListeners.inform(e);
        }
        insertionsListeners.informJobInserted(unassignedJob, inRoute, iData);
    }

}

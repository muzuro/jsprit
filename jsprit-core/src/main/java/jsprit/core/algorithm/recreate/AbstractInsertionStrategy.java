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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import jsprit.core.algorithm.recreate.listener.InsertionListener;
import jsprit.core.algorithm.recreate.listener.InsertionListeners;
import jsprit.core.algorithm.state.DestinationBaseLoadChecker;
import jsprit.core.algorithm.state.UpdateActivityTimes;
import jsprit.core.problem.AbstractActivity;
import jsprit.core.problem.Capacity;
import jsprit.core.problem.Location;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import jsprit.core.problem.driver.Driver;
import jsprit.core.problem.driver.DriverImpl;
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
import jsprit.core.util.RouteUtils;

import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        vrp.getDestinationBaseLoadChecker().refreshFreeJobs(vehicleRoutes);
        Collection<Job> badJobs = insertUnassignedJobs(vehicleRoutes, notBases);
        insertionsListeners.informInsertionEndsListeners(vehicleRoutes);
        vehicleRoutes.forEach(route->{
            optimizeBases(route);
            new RouteActivityVisitor().addActivityVisitor(new UpdateActivityTimes(vrp.getTransportCosts(),
                    ActivityPolicy.AS_SOON_AS_ARRIVED)).visit(route);
        });
        
        return badJobs;
    }
    
    public void optimizeBases(VehicleRoute aRoute) {
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
        while (!routeEnd) {
            if (iterator.hasNext()) {
                next = iterator.next();
            } else {
                next = end;
                routeEnd = true;
            }
            if (current instanceof DestinationService) {
                runLoad = Capacity.addup(runLoad, current.getSize());
            } else if (current instanceof BaseService) {
                if (Objects.isNull(prev) || Objects.isNull(current)) {
                    continue;
                }
                Location bestBaseLocation = findBestLocation(aRoute, prev, next, runLoad, current.getLocation());
                Base base = ((Base) ((BaseService) current).getJob());
                base.setLocation(bestBaseLocation);
            }
            prev = current;
            current = next;
        }
    }
    
    private Location findBestLocation(VehicleRoute aRoute, TourActivity prev, TourActivity next, Capacity aRunLoad,
            Location aCurrentUnloadLocation) {
        DestinationBaseLoadChecker destinationBaseLoadChecker = vrp.getDestinationBaseLoadChecker();
        VehicleRoutingTransportCosts transportCosts = vrp.getTransportCosts();
        Double min = Double.MAX_VALUE;
        Location bestBaseLocation = null;
        for (Location possibleBaseLocation : destinationBaseLoadChecker.getBaseLocations(aRoute.getVehicle())) {
            double toBase = transportCosts.getTransportCost(prev.getLocation(),
                    possibleBaseLocation, prev.getEndTime(), aRoute.getDriver(), aRoute.getVehicle());
            double transportTime = transportCosts.getTransportTime(prev.getLocation(), possibleBaseLocation, prev.getEndTime(),
                    aRoute.getDriver(), aRoute.getVehicle());
            Double mpsProceedTime = destinationBaseLoadChecker.getUnloadDuration(aRoute.getVehicle());
            double fromBaseEndTime = prev.getEndTime() + transportTime + mpsProceedTime;
            double fromBase = transportCosts.getTransportCost(possibleBaseLocation,
                    next.getLocation(), fromBaseEndTime, aRoute.getDriver(), aRoute.getVehicle());
            double total = toBase + fromBase;
            if (total < min && destinationBaseLoadChecker.isLocationDailyLoadable(possibleBaseLocation, aRunLoad)) {
                bestBaseLocation = possibleBaseLocation;
                min = total; 
            }
        }
        return bestBaseLocation != null ? bestBaseLocation : aCurrentUnloadLocation;
    }

    public abstract Collection<Job> insertUnassignedJobs(Collection<VehicleRoute> vehicleRoutes, Collection<Job> unassignedJobs);

    protected void markRequiredRoutes(Collection<VehicleRoute> routes, List<Job> jobs) {
        DestinationBaseLoadChecker destinationBaseLoadChecker = vrp.getDestinationBaseLoadChecker();
        for (VehicleRoute route : routes) {
            boolean anySuitable = jobs.stream().anyMatch(j->!destinationBaseLoadChecker.isLoaded(j, route));
            destinationBaseLoadChecker.markBaseRequired(route, !anySuitable);
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

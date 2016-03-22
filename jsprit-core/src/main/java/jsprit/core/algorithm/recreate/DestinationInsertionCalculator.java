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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import jsprit.core.algorithm.state.DestinationBaseLoadChecker;
import jsprit.core.problem.AbstractActivity;
import jsprit.core.problem.JobActivityFactory;
import jsprit.core.problem.Location;
import jsprit.core.problem.constraint.ConstraintManager;
import jsprit.core.problem.constraint.HardActivityConstraint;
import jsprit.core.problem.constraint.HardActivityConstraint.ConstraintsStatus;
import jsprit.core.problem.constraint.HardRouteConstraint;
import jsprit.core.problem.constraint.SoftActivityConstraint;
import jsprit.core.problem.constraint.SoftRouteConstraint;
import jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import jsprit.core.problem.driver.Driver;
import jsprit.core.problem.job.Base;
import jsprit.core.problem.job.Destination;
import jsprit.core.problem.job.Job;
import jsprit.core.problem.misc.DestinationBaseContext;
import jsprit.core.problem.misc.JobInsertionContext;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.BaseService;
import jsprit.core.problem.solution.route.activity.DefaultTourActivityFactory;
import jsprit.core.problem.solution.route.activity.End;
import jsprit.core.problem.solution.route.activity.Start;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.vehicle.Vehicle;
import jsprit.core.problem.vehicle.VehicleImpl.NoVehicle;
import jsprit.core.util.CalculationUtils;

/**
 * Calculator that calculates the best insertion position for a {@link Destination}.
 */
final class DestinationInsertionCalculator implements JobInsertionCostsCalculator {

    private static final Logger logger = LogManager.getLogger(DestinationInsertionCalculator.class);

    private HardRouteConstraint hardRouteLevelConstraint;

    private HardActivityConstraint hardActivityLevelConstraint;

    private SoftRouteConstraint softRouteConstraint;

    private SoftActivityConstraint softActivityConstraint;

    private VehicleRoutingTransportCosts transportCosts;

    private ActivityInsertionCostsCalculator additionalTransportCostsCalculator;

    private JobActivityFactory activityFactory;

    private AdditionalAccessEgressCalculator additionalAccessEgressCalculator;

    private DestinationBaseLoadChecker destinationBaseLoadChecker;

    private DefaultTourActivityFactory serviceActivityFactory;

    public DestinationInsertionCalculator(VehicleRoutingTransportCosts routingCosts, ActivityInsertionCostsCalculator
            additionalTransportCostsCalculator, ConstraintManager constraintManager,
            JobActivityFactory aActivityFactory, DestinationBaseLoadChecker aDestinationBaseLoadChecker) {
        super();
        this.transportCosts = routingCosts;
        hardRouteLevelConstraint = constraintManager;
        hardActivityLevelConstraint = constraintManager;
        softActivityConstraint = constraintManager;
        softRouteConstraint = constraintManager;
        this.additionalTransportCostsCalculator = additionalTransportCostsCalculator;
        activityFactory = aActivityFactory;
        destinationBaseLoadChecker = aDestinationBaseLoadChecker;
        additionalAccessEgressCalculator = new AdditionalAccessEgressCalculator(routingCosts);
        serviceActivityFactory = new DefaultTourActivityFactory();
        logger.debug("initialise {}", this);
    }

    @Override
    public String toString() {
        return "[name=calculatesServiceInsertion]";
    }

    /**
     * Calculates the marginal cost of inserting job i locally. This is based on the
     * assumption that cost changes can entirely covered by only looking at the predecessor i-1 and its successor i+1.
     */
    @Override
    public InsertionData getInsertionData(final VehicleRoute currentRoute, final Job jobToInsert, final Vehicle newVehicle, double newVehicleDepartureTime, final Driver newDriver, final double bestKnownCosts) {
        JobInsertionContext insertionContext = new JobInsertionContext(currentRoute, jobToInsert, newVehicle, newDriver, newVehicleDepartureTime);
        DestinationBaseContext destinationBaseContext = new DestinationBaseContext();
        insertionContext.setDestinationBaseContext(destinationBaseContext);
        Destination destination = (Destination) jobToInsert;
        int insertionIndex = InsertionData.NO_INDEX;

        TourActivity deliveryAct2Insert = activityFactory.createActivities(destination).get(0);
        insertionContext.getAssociatedActivities().add(deliveryAct2Insert);

        /*
        check hard constraints at route level
         */
        if (!hardRouteLevelConstraint.fulfilled(insertionContext)) {
            return InsertionData.createEmptyInsertionData();
        }

        /*
        check soft constraints at route level
         */
        double additionalICostsAtRouteLevel = softRouteConstraint.getCosts(insertionContext);

        double bestCost = bestKnownCosts;
        additionalICostsAtRouteLevel += additionalAccessEgressCalculator.getCosts(insertionContext);
        
        /*
        generate new start and end for new vehicle
         */
        Start start = new Start(newVehicle.getStartLocation(), newVehicle.getEarliestDeparture(), Double.MAX_VALUE);
        start.setEndTime(newVehicleDepartureTime);
        End end = new End(newVehicle.getEndLocation(), 0.0, newVehicle.getLatestArrival());
        
        if (destinationBaseLoadChecker.isBaseRequired(currentRoute)) {
            List<TourActivity> activities = currentRoute.getActivities();
            TourActivity prevAct;
            if (activities.isEmpty()) {
                prevAct = start;
                insertionIndex = 0;
            } else {
                insertionIndex = activities.size() - 1;
                prevAct = activities.get(insertionIndex);
            }
            
            Location bestBaseLocation = findBestLocation(currentRoute, newVehicle, newDriver, deliveryAct2Insert, end);
            if (bestBaseLocation != null) {              
                Integer runNumber = destinationBaseLoadChecker.getRunCount(currentRoute);
                if (Objects.isNull(runNumber)) {
                    runNumber = 0;
                }
                
                Base selectedBase = destinationBaseLoadChecker.findBaseToInsert();
                selectedBase.setLocation(bestBaseLocation);
                selectedBase.setServiceDuration(destinationBaseLoadChecker.getUnloadDuration(newVehicle));
                AbstractActivity baseActivity = serviceActivityFactory.createActivity(selectedBase);
                ConstraintsStatus actStatus = hardActivityLevelConstraint.fulfilled(insertionContext, prevAct, deliveryAct2Insert,
                        baseActivity, prevAct.getEndTime());
                
                double actArrTime = prevAct.getEndTime() + transportCosts.getTransportTime(prevAct.getLocation(),
                        deliveryAct2Insert.getLocation(), prevAct.getEndTime(), newDriver, newVehicle);
                double actDepTime = CalculationUtils.getActivityEndTime(actArrTime, deliveryAct2Insert);
                ConstraintsStatus baseStatus = hardActivityLevelConstraint.fulfilled(insertionContext, deliveryAct2Insert,
                        baseActivity, end, actDepTime);
                boolean canCreateNextRun = actStatus == ConstraintsStatus.NOT_FULFILLED_BREAK
                        || actStatus == ConstraintsStatus.FULFILLED;
                if (canCreateNextRun && baseStatus == ConstraintsStatus.FULFILLED) {
                    double additionalICostsAtActLevel = softActivityConstraint.getCosts(insertionContext, prevAct, deliveryAct2Insert, baseActivity, prevAct.getEndTime());
                    double additionalTransportationCosts = additionalTransportCostsCalculator.getCosts(insertionContext, prevAct, deliveryAct2Insert, baseActivity, prevAct.getEndTime());
                    double iterCost = additionalICostsAtRouteLevel + additionalICostsAtActLevel + additionalTransportationCosts;
                    
                    int deliveryInsertionIndex = insertionIndex + 1;
                    InsertionData insertionData = new InsertionData(iterCost, InsertionData.NO_INDEX, deliveryInsertionIndex, newVehicle, newDriver);
                    insertionData.setInsertionRunNumber(runNumber);
                    insertionData.getEvents().add(new InsertActivity(currentRoute, newVehicle, deliveryAct2Insert,
                            deliveryInsertionIndex, baseActivity, bs->destinationBaseLoadChecker.takeBase(selectedBase)));
                    insertionData.getEvents().add(new SwitchVehicle(currentRoute, newVehicle, newVehicleDepartureTime));
                    insertionData.setVehicleDepartureTime(newVehicleDepartureTime);
                    insertionData.setBaseToInsert(selectedBase);
                    return insertionData;
                }
            }
            logger.debug("Can`t insert destination {} with base. route {}", jobToInsert.getName(),
                    currentRoute.prettyPrintActivitesWithCapacity());
            return InsertionData.createEmptyInsertionData();
        }

        TourActivity prevAct = start;
        double prevActDepTime = newVehicleDepartureTime;
        int actIndex = 0;
        Iterator<TourActivity> activityIterator = currentRoute.getActivities().iterator();
        boolean tourEnd = false;
        boolean rewindRun = false;
//        StringBuilder sb = null;
//        if (!currentRoute.prettyPrintActivites().contains("dtw")
//                //&& currentRoute.prettyPrintActivites().contains("dtw2")
//                && deliveryAct2Insert.getName().contains("dtw")) {
//            sb = new StringBuilder("\n");
//        }
        Consumer<TourActivity> baseLocationUpdater = EmptyConsumer.INSTANCE;
        int insertionRunNumber = -1;
        while (!tourEnd) {
            TourActivity nextAct;
            if (activityIterator.hasNext()) {
                nextAct = activityIterator.next();
            } else {
                nextAct = end;
                tourEnd = true;
            }
            
            
            if (prevAct instanceof BaseService) {
                destinationBaseContext.incrementRunNum();
                rewindRun = false;
            }
            
            if (!rewindRun) {//omit constraints check for full runs                 
                ConstraintsStatus status = hardActivityLevelConstraint.fulfilled(insertionContext, prevAct, deliveryAct2Insert, nextAct, prevActDepTime);
                if (status.equals(ConstraintsStatus.FULFILLED)) {
                    //from job2insert induced costs at activity level
//                    Location currentBaseLocation = null;
//                    Location bestBaseLocation = null;
//                    if (nextAct instanceof BaseService) {
//                        int afterBaseIndex = actIndex + 1;
//                        TourActivity afterBaseAct;
//                        if (currentRoute.getActivities().size() == afterBaseIndex) {
//                            afterBaseAct = end;
//                        } else {
//                            afterBaseAct = currentRoute.getActivities().get(afterBaseIndex);
//                        }
//                        bestBaseLocation = findBestLocation(currentRoute, newVehicle, newDriver, deliveryAct2Insert, afterBaseAct);
//                        Base base = (Base) ((BaseService) nextAct).getJob();
//                        currentBaseLocation = base.getLocation();
//                        base.setLocation(bestBaseLocation);
//                    }
                    double additionalICostsAtActLevel = softActivityConstraint.getCosts(insertionContext, prevAct, deliveryAct2Insert, nextAct, prevActDepTime);
                    double additionalTransportationCosts = additionalTransportCostsCalculator.getCosts(insertionContext, prevAct, nextAct, deliveryAct2Insert, prevActDepTime);
                    double iterCost = additionalICostsAtRouteLevel + additionalICostsAtActLevel + additionalTransportationCosts;
//                    if (nextAct instanceof BaseService) {
//                        Base base = (Base) ((BaseService) nextAct).getJob();
//                        base.setLocation(currentBaseLocation);
//                    }
                    if (iterCost < bestCost) {
                        bestCost = iterCost;
                        insertionIndex = actIndex;
                        insertionRunNumber  = destinationBaseContext.getRunNum();
//                        if (nextAct instanceof BaseService) {
//                            Location toChange = bestBaseLocation; 
//                            baseLocationUpdater = new Consumer<TourActivity>() {
//                                @Override
//                                public void accept(TourActivity aT) {
//                                    Base base = (Base) ((BaseService) nextAct).getJob();
//                                    base.setLocation(toChange);
//                                }
//                            };
//                        } else {
//                            baseLocationUpdater = EmptyConsumer.INSTANCE;
//                        }
                    }
//                    if (sb != null) {
//                       sb.append(String.format("%s|%s|%s|%s|%s|%s", actIndex, destinationBaseContext.getRunNum(), iterCost,
//                               bestCost, additionalICostsAtActLevel, additionalTransportationCosts));
//                       sb.append("\n");
//                    }
                } else if (status.equals(ConstraintsStatus.NOT_FULFILLED_BREAK)) {
                    //it means run is full, we shouldn`t check other destinations in current run
                    rewindRun = true;
                }
            }
            double nextActArrTime = prevActDepTime + transportCosts.getTransportTime(prevAct.getLocation(), nextAct.getLocation(), prevActDepTime, newDriver, newVehicle);
            prevActDepTime = CalculationUtils.getActivityEndTime(nextActArrTime, nextAct);
            prevAct = nextAct;
            actIndex++;
            destinationBaseContext.incrementInsertionIndex();
        }
//        if (sb != null) {
//            logger.info(currentRoute.prettyPrintActivites());
//            logger.info("{} possible insert position {}", deliveryAct2Insert.getName(), insertionIndex);
//            logger.info(sb.toString());
//        }
        if (insertionIndex == InsertionData.NO_INDEX) {
            logger.debug("Can`t insert destination {}. route {}", jobToInsert.getName(),
                    currentRoute.prettyPrintActivitesWithCapacity());
            return InsertionData.createEmptyInsertionData();
        }
        InsertionData insertionData = new InsertionData(bestCost, InsertionData.NO_INDEX, insertionIndex, newVehicle, newDriver);
        insertionData.setInsertionRunNumber(insertionRunNumber);
        insertionData.getEvents().add(new InsertActivity(currentRoute, newVehicle, deliveryAct2Insert, insertionIndex));
        insertionData.getEvents().add(new SwitchVehicle(currentRoute, newVehicle, newVehicleDepartureTime));
        insertionData.setVehicleDepartureTime(newVehicleDepartureTime);
        return insertionData;
    }

    private Location findBestLocation(final VehicleRoute currentRoute, final Vehicle newVehicle, final Driver newDriver,
            TourActivity aPrevBaseAct, TourActivity aPostBaseAct) {
        List<Location> baseLocations;
        if (currentRoute.getVehicle() instanceof NoVehicle) {
            //later base location will be obtained from vehicle(while base optimization jsprit.core.algorithm.recreate.AbstractInsertionStrategy.optimizeBases(VehicleRoute))
            baseLocations = destinationBaseLoadChecker.getAllVehicleBaseLocations();
        } else {
            baseLocations = destinationBaseLoadChecker.getBaseLocations(currentRoute.getVehicle());
        }
        Double min = Double.MAX_VALUE;
        
        Location bestBaseLocation = null;
        for (Location possibleBaseLocation : baseLocations) {
            double toBase = transportCosts.getTransportCost(aPrevBaseAct.getLocation(),
                    possibleBaseLocation, 0d, newDriver, newVehicle);
            double fromBase = transportCosts.getTransportCost(possibleBaseLocation,
                    aPostBaseAct.getLocation(), 0d, newDriver, newVehicle);
            double transportCost = toBase + fromBase; 
            if (transportCost < min && 
                    destinationBaseLoadChecker.isLocationDailyLoadable(possibleBaseLocation, aPrevBaseAct.getSize())) {
                bestBaseLocation = possibleBaseLocation;
                min = transportCost; 
            }
        }
        return bestBaseLocation;
    }
    
    public static class EmptyConsumer implements Consumer<TourActivity> {
        public static final EmptyConsumer INSTANCE = new EmptyConsumer();
        private EmptyConsumer() {
        }
        @Override
        public void accept(TourActivity aT) {
        }
    }

}

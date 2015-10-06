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

import java.util.Iterator;

import jsprit.core.problem.JobActivityFactory;
import jsprit.core.problem.constraint.ConstraintManager;
import jsprit.core.problem.constraint.HardActivityConstraint;
import jsprit.core.problem.constraint.HardActivityConstraint.ConstraintsStatus;
import jsprit.core.problem.constraint.HardRouteConstraint;
import jsprit.core.problem.constraint.SoftActivityConstraint;
import jsprit.core.problem.constraint.SoftRouteConstraint;
import jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import jsprit.core.problem.driver.Driver;
import jsprit.core.problem.job.Destination;
import jsprit.core.problem.job.Job;
import jsprit.core.problem.misc.DestinationBaseContext;
import jsprit.core.problem.misc.JobInsertionContext;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.BaseService;
import jsprit.core.problem.solution.route.activity.End;
import jsprit.core.problem.solution.route.activity.Start;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.vehicle.Vehicle;
import jsprit.core.util.CalculationUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    public DestinationInsertionCalculator(VehicleRoutingTransportCosts routingCosts, ActivityInsertionCostsCalculator
            additionalTransportCostsCalculator, ConstraintManager constraintManager, JobActivityFactory aActivityFactory) {
        super();
        this.transportCosts = routingCosts;
        hardRouteLevelConstraint = constraintManager;
        hardActivityLevelConstraint = constraintManager;
        softActivityConstraint = constraintManager;
        softRouteConstraint = constraintManager;
        this.additionalTransportCostsCalculator = additionalTransportCostsCalculator;
        activityFactory = aActivityFactory;
        additionalAccessEgressCalculator = new AdditionalAccessEgressCalculator(routingCosts);
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

        TourActivity prevAct = start;
        double prevActStartTime = newVehicleDepartureTime;
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
        while (!tourEnd) {
            TourActivity nextAct;
            if (activityIterator.hasNext()) {
                nextAct = activityIterator.next();
            } else {
                nextAct = end;
                tourEnd = true;
            }
            
            if (nextAct instanceof BaseService) {
                rewindRun = false;
            }
            
            if (prevAct instanceof BaseService) {
                destinationBaseContext.incrementRunNum();
            }
            
            if (!rewindRun) {//omit constraints check for full runs                 
                ConstraintsStatus status = hardActivityLevelConstraint.fulfilled(insertionContext, prevAct, deliveryAct2Insert, nextAct, prevActStartTime);
                if (status.equals(ConstraintsStatus.FULFILLED)) {
                    //from job2insert induced costs at activity level
                    double additionalICostsAtActLevel = softActivityConstraint.getCosts(insertionContext, prevAct, deliveryAct2Insert, nextAct, prevActStartTime);
                    double additionalTransportationCosts = additionalTransportCostsCalculator.getCosts(insertionContext, prevAct, nextAct, deliveryAct2Insert, prevActStartTime);
                    double iterCost = additionalICostsAtRouteLevel + additionalICostsAtActLevel + additionalTransportationCosts;
                    if (iterCost < bestCost) {
                        bestCost = iterCost;
                        insertionIndex = actIndex;
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
            double nextActArrTime = prevActStartTime + transportCosts.getTransportTime(prevAct.getLocation(), nextAct.getLocation(), prevActStartTime, newDriver, newVehicle);
            prevActStartTime = CalculationUtils.getActivityEndTime(nextActArrTime, nextAct);
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
            logger.debug("Can`t insert destination {}. route {}", jobToInsert.getName(), currentRoute.prettyPrintActivitesWithCapacity());
            return InsertionData.createEmptyInsertionData();
        }
        InsertionData insertionData = new InsertionData(bestCost, InsertionData.NO_INDEX, insertionIndex, newVehicle, newDriver);
        insertionData.getEvents().add(new InsertActivity(currentRoute, newVehicle, deliveryAct2Insert, insertionIndex));
        insertionData.getEvents().add(new SwitchVehicle(currentRoute, newVehicle, newVehicleDepartureTime));
        insertionData.setVehicleDepartureTime(newVehicleDepartureTime);
        return insertionData;
    }
    
    

}

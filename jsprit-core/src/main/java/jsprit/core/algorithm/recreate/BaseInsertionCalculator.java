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
import jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import jsprit.core.problem.driver.Driver;
import jsprit.core.problem.job.Base;
import jsprit.core.problem.job.Job;
import jsprit.core.problem.misc.DestinationBaseContext;
import jsprit.core.problem.misc.JobInsertionContext;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.End;
import jsprit.core.problem.solution.route.activity.Start;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.vehicle.Vehicle;
import jsprit.core.util.CalculationUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class BaseInsertionCalculator implements JobInsertionCostsCalculator {

    private static final Logger logger = LogManager.getLogger(BaseInsertionCalculator.class);

    private HardRouteConstraint hardRouteLevelConstraint;

    private HardActivityConstraint hardActivityLevelConstraint;

    private VehicleRoutingTransportCosts transportCosts;

    private JobActivityFactory activityFactory;

    public BaseInsertionCalculator(VehicleRoutingTransportCosts routingCosts, ConstraintManager constraintManager,
            JobActivityFactory jobActivityFactory) {
        super();
        this.transportCosts = routingCosts;
        hardRouteLevelConstraint = constraintManager;
        hardActivityLevelConstraint = constraintManager;
        activityFactory = jobActivityFactory;
        logger.debug("initialise {}", this);
    }

    @Override
    public String toString() {
        return "[name=calculatesServiceInsertion]";
    }

    /**
     * If job fullfiled constraints - it always return Double.MIN_VALUE cost, becouse bases should be inserted at first 
     */
    @Override
    public InsertionData getInsertionData(final VehicleRoute currentRoute, final Job jobToInsert, final Vehicle newVehicle, double newVehicleDepartureTime, final Driver newDriver, final double bestKnownCosts) {
        JobInsertionContext insertionContext = new JobInsertionContext(currentRoute, jobToInsert, newVehicle, newDriver, newVehicleDepartureTime);
        DestinationBaseContext destinationBaseContext = new DestinationBaseContext();
        insertionContext.setDestinationBaseContext(destinationBaseContext);
        Base base = (Base) jobToInsert;

        TourActivity deliveryAct2Insert = activityFactory.createActivities(base).get(0);
        insertionContext.getAssociatedActivities().add(deliveryAct2Insert);

        /*
        check hard constraints at route level
         */
        if (!hardRouteLevelConstraint.fulfilled(insertionContext)) {
            return InsertionData.createEmptyInsertionData();
        }

        /*
        generate new start and end for new vehicle
         */
        Start start = new Start(newVehicle.getStartLocation(), newVehicle.getEarliestDeparture(), Double.MAX_VALUE);
        start.setEndTime(newVehicleDepartureTime);
        End end = new End(newVehicle.getEndLocation(), 0.0, newVehicle.getLatestArrival());

        TourActivity prevAct = start;
        double prevActStartTime = newVehicleDepartureTime;
        Iterator<TourActivity> activityIterator = currentRoute.getActivities().iterator();
        boolean tourEnd = false;
        
        int insertionIndex = 0;
        while (!tourEnd) {
            TourActivity nextAct;
            if (activityIterator.hasNext()) {
                nextAct = activityIterator.next();
            } else {
                nextAct = end;
                tourEnd = true;
            }
            
            ConstraintsStatus status = hardActivityLevelConstraint.fulfilled(insertionContext, prevAct, deliveryAct2Insert,
                    nextAct, prevActStartTime);
            
            if (status.equals(ConstraintsStatus.FULFILLED)) {
                InsertionData insertionData = new InsertionData(0, InsertionData.NO_INDEX, insertionIndex, newVehicle, newDriver);
                insertionData.getEvents().add(new InsertActivity(currentRoute, newVehicle, deliveryAct2Insert, insertionIndex));
                insertionData.getEvents().add(new SwitchVehicle(currentRoute, newVehicle, newVehicleDepartureTime));
                insertionData.setVehicleDepartureTime(newVehicleDepartureTime);
                return insertionData;
            } else if (status.equals(ConstraintsStatus.NOT_FULFILLED_BREAK)) {
                break;
            }
            double nextActArrTime = prevActStartTime + transportCosts.getTransportTime(prevAct.getLocation(), nextAct.getLocation(),
                    prevActStartTime, newDriver, newVehicle);
            prevActStartTime = CalculationUtils.getActivityEndTime(nextActArrTime, nextAct);
            prevAct = nextAct;
            insertionIndex++;
            destinationBaseContext.incrementInsertionIndex();
        }
        logger.info("Can`t insert base {}. route {}", jobToInsert.getName(), currentRoute.prettyPrintActivites());
        return InsertionData.createEmptyInsertionData();
    }

}

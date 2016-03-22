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
package jsprit.core.algorithm.state;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import jsprit.core.algorithm.recreate.InsertionData;
import jsprit.core.algorithm.recreate.listener.JobInsertedListener;
import jsprit.core.algorithm.ruin.listener.RuinListener;
import jsprit.core.problem.job.Job;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.DestinationService;
import jsprit.core.problem.solution.route.activity.PickupService;
import jsprit.core.problem.solution.route.activity.TimeWindow;

public class UpdateDestinationTimeMiss implements JobInsertedListener, RuinListener {

    private StateManager stateManager;

    public UpdateDestinationTimeMiss(StateManager stateManager) {
        this.stateManager = stateManager;
    }

    @Override
    public void informJobInserted(Job aInsertedJob, VehicleRoute aInRoute, InsertionData aIData) {
        refreshRuns(aInRoute);
    }
    
    @Override
    public void ruinEnds(Collection<VehicleRoute> routes, Collection<Job> unassignedJobs) {
        for (VehicleRoute route : routes) {
            refreshRuns(route);
        }
    }
    
    private void refreshRuns(VehicleRoute route) {
        List<PickupService> activities = (List) route.getActivities();
        ListIterator<PickupService> listIterator = activities.listIterator();
        
        double destinationCount = 0;
        double destinationTwLessCount = 0;
        
        double pastLateness = 0;
        double pastWaiting = 0;
        
        while (listIterator.hasNext()) {
            PickupService ta = listIterator.next();
            if (!(ta instanceof DestinationService)) {
                continue;
            }
            destinationCount++;
            stateManager.putInternalTypedActivityState(ta, InternalStates.DestinationBase.PAST_LATENESS, pastLateness);
            stateManager.putInternalTypedActivityState(ta, InternalStates.DestinationBase.PAST_WAITING, pastWaiting);
            if (!TimeWindowMissUtils.isDefaultTimeWindow(ta.getJob().getTimeWindow())) {
                pastWaiting += TimeWindowMissUtils.calculateStartMissSecond(ta, route.getVehicle());
                pastLateness += TimeWindowMissUtils.calculateEndMissSecond(ta, route.getVehicle());
            } else {
                destinationTwLessCount++;
            }
        }
        
        stateManager.putTypedInternalRouteState(route, InternalStates.DestinationBase.PAST_LATENESS, pastLateness);
        stateManager.putTypedInternalRouteState(route, InternalStates.DestinationBase.PAST_WAITING, pastWaiting);
        
        if (destinationCount != 0) {
            double timewindowlessCountWeigth = destinationTwLessCount / destinationCount;
            stateManager.putTypedInternalRouteState(route, InternalStates.DestinationBase.TIMEWINDOWLESS_COUNT_WEIGTH,
                    timewindowlessCountWeigth);
        } else {
            stateManager.putTypedInternalRouteState(route, InternalStates.DestinationBase.TIMEWINDOWLESS_COUNT_WEIGTH,
                    0d);
        }
        
        double futureLateness = 0;
        double futureWaiting = 0;
        while (listIterator.hasPrevious()) {
            PickupService ta = listIterator.previous();
            stateManager.putInternalTypedActivityState(ta, InternalStates.DestinationBase.FUTURE_LATENESS, futureLateness);
            stateManager.putInternalTypedActivityState(ta, InternalStates.DestinationBase.FUTURE_WAITING, futureWaiting);
            if (ta instanceof DestinationService && !TimeWindowMissUtils.isDefaultTimeWindow(ta.getJob().getTimeWindow())) {
                futureWaiting += TimeWindowMissUtils.calculateStartMissSecond(ta, route.getVehicle());
                futureLateness += TimeWindowMissUtils.calculateEndMissSecond(ta, route.getVehicle());
            }
        }
        
        stateManager.putTypedInternalRouteState(route, InternalStates.DestinationBase.FUTURE_LATENESS, futureLateness);
        stateManager.putTypedInternalRouteState(route, InternalStates.DestinationBase.FUTURE_WAITING, futureWaiting);
    }
    
    @Override
    public void removed(Job job, VehicleRoute fromRoute) {
        // TODO Auto-generated method stub
    }
    
    @Override
    public void ruinStarts(Collection<VehicleRoute> routes) {
        // TODO Auto-generated method stub
    }

}

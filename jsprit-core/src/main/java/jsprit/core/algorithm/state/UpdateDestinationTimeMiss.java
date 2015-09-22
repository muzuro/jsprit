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

import jsprit.core.algorithm.recreate.listener.JobInsertedListener;
import jsprit.core.algorithm.ruin.listener.RuinListener;
import jsprit.core.problem.Location;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import jsprit.core.problem.job.Job;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.DestinationService;
import jsprit.core.problem.solution.route.activity.PickupService;
import jsprit.core.problem.solution.route.activity.TimeWindow;

public class UpdateDestinationTimeMiss implements JobInsertedListener, RuinListener {

    private StateManager stateManager;
    private VehicleRoutingProblem vrp;

    public UpdateDestinationTimeMiss(StateManager stateManager, VehicleRoutingProblem aVrp) {
        this.stateManager = stateManager;
        vrp = aVrp;
    }

    

    @Override
    public void informJobInserted(Job job2insert, VehicleRoute inRoute, double additionalCosts, double additionalTime) {
        refreshRuns(inRoute);
    }

    @Override
    public void ruinEnds(Collection<VehicleRoute> routes, Collection<Job> unassignedJobs) {
        for (VehicleRoute route : routes) {
            refreshRuns(route);
        }
    }
    
    void refreshRuns(VehicleRoute route) {
        VehicleRoutingTransportCosts transportCosts = vrp.getTransportCosts();
        Location prevPoint = route.getStart().getLocation();
        
        List<PickupService> activities = (List) route.getActivities();
        double time = route.getVehicle().getEarliestDeparture();
        for (PickupService ta : activities) {
            Location nextPoint = ta.getLocation();
            time += transportCosts.getTransportTime(prevPoint, nextPoint, time, route.getDriver(), route.getVehicle());
            if (ta instanceof DestinationService) {                
                double startMiss = calculateStartMissSecond(ta, time);
                double endMiss = calculateEndMissSecond(ta, time);
                stateManager.putInternalTypedActivityState(ta, InternalStates.TIME_SLACK, new TimeMissInfo(startMiss,
                        endMiss));
            }
            time += ta.getOperationTime();
        }
        
    }
    
    private double calculateStartMissSecond(PickupService aAct, double aArrivalTime) {
        TimeWindow timeWindow = aAct.getJob().getTimeWindow();
        return timeWindow.getStart() - aArrivalTime;//отрицательное время - значит есть запас 
    }
    
    private double calculateEndMissSecond(PickupService aAct, double aArrivalTime) {
        TimeWindow timeWindow = aAct.getJob().getTimeWindow();
        return aArrivalTime - timeWindow.getEnd();//отрицательное время - значит есть запас 
    }
    
    public static class TimeMissInfo {
        private final double startMiss;
        private final double endMiss;
        public TimeMissInfo(double startMiss, double endMiss) {
            super();
            this.startMiss = startMiss;
            this.endMiss = endMiss;
        }
        public double getStartMiss() {
            return startMiss;
        }
        public double getEndMiss() {
            return endMiss;
        }
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

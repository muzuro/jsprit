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

import jsprit.core.algorithm.recreate.listener.InsertionStartsListener;
import jsprit.core.algorithm.recreate.listener.JobInsertedListener;
import jsprit.core.algorithm.ruin.listener.RuinListener;
import jsprit.core.problem.Capacity;
import jsprit.core.problem.job.Base;
import jsprit.core.problem.job.Destination;
import jsprit.core.problem.job.Job;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.ActivityVisitor;
import jsprit.core.problem.solution.route.activity.PickupService;
import jsprit.core.problem.solution.route.activity.TourActivity;

class UpdateDestinationBaseLoads implements /*ActivityVisitor, StateUpdater, InsertionStartsListener, */JobInsertedListener, RuinListener {

    private StateManager stateManager;

    /*
     * default has one dimension with a value of zero
     */
    private Capacity currentLoad;

    private Capacity defaultValue;


    public UpdateDestinationBaseLoads(StateManager stateManager) {
        super();
        this.stateManager = stateManager;
        defaultValue = Capacity.Builder.newInstance().build();
    }

//    @Override
//    public void informInsertionStarts(Collection<VehicleRoute> vehicleRoutes, Collection<Job> unassignedJobs) {
////        for (VehicleRoute route : vehicleRoutes) {
////            insertionStarts(route);
////        }
//    }
    
    void refreshRuns(VehicleRoute route) {
        Capacity runLoad = Capacity.Builder.newInstance().build();
        int runNum = 0;
        List<PickupService> activities = (List) route.getActivities();
        for (PickupService ta : activities) {
            Job j = ta.getJob();
            if (j instanceof Destination) {
                runLoad = Capacity.addup(runLoad, j.getSize());
                stateManager.putInternalTypedActivityState(ta, InternalStates.FUTURE_MAXLOAD, Capacity.copyOf(runLoad));
            } else if (j instanceof Base) {
                stateManager.putRunState(route, runNum, InternalStates.DestinationBase.RUN_LOAD, Capacity.copyOf(runLoad));
                runLoad = Capacity.Builder.newInstance().build();
                runNum++;
            }
        }
        
    }

//    @Override
//    public void begin(VehicleRoute route) {
////        currentLoad = stateManager.getRouteState(route, InternalStates.LOAD_AT_BEGINNING, Capacity.class);
////        if (currentLoad == null) currentLoad = defaultValue;
//    }
//
//    @Override
//    public void visit(TourActivity act) {
////        currentLoad = Capacity.addup(currentLoad, act.getSize());
////        stateManager.putInternalTypedActivityState(act, InternalStates.LOAD, currentLoad);
//    }
//
//    @Override
//    public void finish() {
////        currentLoad = Capacity.Builder.newInstance().build();
//    }

    @Override
    public void informJobInserted(Job job2insert, VehicleRoute inRoute, double additionalCosts, double additionalTime) {
        refreshRuns(inRoute);
//        if (job2insert instanceof Base) {
//        } else if (job2insert instanceof Destination) {
//            System.out.println(job2insert);
////            Capacity loadAtEnd = stateManager.getRouteState(inRoute, InternalStates.LOAD_AT_END, Capacity.class);
////            if (loadAtEnd == null) loadAtEnd = defaultValue;
////            stateManager.putTypedInternalRouteState(inRoute, InternalStates.LOAD_AT_END, Capacity.addup(loadAtEnd, job2insert.getSize()));
//        }
    }

    @Override
    public void ruinStarts(Collection<VehicleRoute> routes) {
        // TODO Auto-generated method stub
    }

    @Override
    public void ruinEnds(Collection<VehicleRoute> routes, Collection<Job> unassignedJobs) {
        for (VehicleRoute route : routes) {
            refreshRuns(route);
        }
    }

    @Override
    public void removed(Job job, VehicleRoute fromRoute) {
        // TODO Auto-generated method stub
    }

}

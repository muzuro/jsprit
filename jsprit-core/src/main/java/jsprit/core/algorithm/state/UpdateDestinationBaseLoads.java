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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import jsprit.core.algorithm.recreate.listener.JobInsertedListener;
import jsprit.core.algorithm.ruin.listener.RuinListener;
import jsprit.core.problem.Capacity;
import jsprit.core.problem.job.Base;
import jsprit.core.problem.job.Destination;
import jsprit.core.problem.job.Job;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.PickupService;

class UpdateDestinationBaseLoads implements JobInsertedListener, RuinListener {

    private StateManager stateManager;
    private DestinationBaseLoadChecker destinationBaseLoadChecker;

    public UpdateDestinationBaseLoads(StateManager stateManager, DestinationBaseLoadChecker destinationBaseLoadChecker) {
        super();
        this.stateManager = stateManager;
        this.destinationBaseLoadChecker = destinationBaseLoadChecker;
    }

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
        stateManager.putTypedInternalRouteState(route, InternalStates.DestinationBase.RUN_COUNT, runNum);
    }

    @Override
    public void informJobInserted(Job job2insert, VehicleRoute inRoute, double additionalCosts, double additionalTime) {
        refreshRuns(inRoute);
    }

    @Override
    public void ruinStarts(Collection<VehicleRoute> routes) {
        // TODO Auto-generated method stub
    }

    @Override
    public void ruinEnds(Collection<VehicleRoute> routes, Collection<Job> unassignedJobs) {
        for (VehicleRoute route : routes) {
            refreshRuns(route);
            clearDoubleBases(route);
        }
    }

    private void clearDoubleBases(VehicleRoute route) {
        List<PickupService> activities = (List) route.getActivities();
        Job prevJob = null;
        List<Base> toDelete = new ArrayList<>(); 
        for (PickupService ta : activities) {
            Job currJob = ta.getJob();
            boolean doubleBases = currJob instanceof Base && prevJob instanceof Base;
            boolean afterStart = currJob instanceof Base && Objects.isNull(prevJob);
            if (doubleBases || afterStart) {
                toDelete.add((Base)currJob);
            }
            prevJob = currJob;
        }
        toDelete.forEach(job->{
            route.getTourActivities().removeJob(job);
            destinationBaseLoadChecker.releaseBase(job);
        });
    }

    @Override
    public void removed(Job job, VehicleRoute fromRoute) {
        // TODO Auto-generated method stub
    }

}

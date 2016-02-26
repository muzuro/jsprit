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
package jsprit.core.problem.constraint;

import java.util.Objects;

import jsprit.core.algorithm.state.InternalStates;
import jsprit.core.problem.Capacity;
import jsprit.core.problem.misc.JobInsertionContext;
import jsprit.core.problem.solution.route.activity.DestinationService;
import jsprit.core.problem.solution.route.activity.End;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.solution.route.state.RouteAndActivityStateGetter;


/**
 * Ensures load constraint for inserting DestinationActivity.
 * <p/>
 *
 */
public class DestinationLoadActivityLevelConstraint implements HardActivityConstraint {

    private RouteAndActivityStateGetter stateManager;

    private Capacity defaultValue;

    private Capacity firstRunCapacity;

    public DestinationLoadActivityLevelConstraint(RouteAndActivityStateGetter stateManager) {
        super();
        this.stateManager = stateManager;
        defaultValue = Capacity.Builder.newInstance().build();
    }
    
    public DestinationLoadActivityLevelConstraint(RouteAndActivityStateGetter stateManager, Capacity aFirstRunCapacity) {
        super();
        this.stateManager = stateManager;
        firstRunCapacity = aFirstRunCapacity;
        defaultValue = Capacity.Builder.newInstance().build();
    }

    @Override
    public ConstraintsStatus fulfilled(JobInsertionContext iFacts, TourActivity prevAct, TourActivity newAct,
            TourActivity nextAct, double prevActDepTime) {
        if (!(newAct instanceof DestinationService)) {
            return ConstraintsStatus.FULFILLED;
        }
//        if (nextAct instanceof End) {
//            return ConstraintsStatus.NOT_FULFILLED_BREAK; 
//        }
        int runNum = iFacts.getDestinationBaseContext().getRunNum();
        Capacity runLoad = stateManager.getRunState(iFacts.getRoute(), runNum, InternalStates.DestinationBase.RUN_LOAD,
                Capacity.class, defaultValue);
        //run is empty and insert before end not allow
        if (nextAct instanceof End/* && runLoad == defaultValue*/) {
            return ConstraintsStatus.NOT_FULFILLED_BREAK;// means not suitible for this run
        }
        //insert allowed if run is not loaded
        Capacity vehicleCapacity = iFacts.getNewVehicle().getType().getCapacityDimensions();
        if (iFacts.getDestinationBaseContext().isFirstRun() && Objects.nonNull(firstRunCapacity)) {
            vehicleCapacity = firstRunCapacity;
        }
        if (Capacity.addup(newAct.getSize(), runLoad).isLessOrEqual(vehicleCapacity)) {
            return ConstraintsStatus.FULFILLED;
        }
        return ConstraintsStatus.NOT_FULFILLED_BREAK;// means not suitible for this run
    }
}

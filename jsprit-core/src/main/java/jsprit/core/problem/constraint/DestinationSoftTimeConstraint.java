package jsprit.core.problem.constraint;

import java.util.Collections;
import java.util.List;

import jsprit.core.algorithm.state.InternalStates;
import jsprit.core.algorithm.state.StateManager;
import jsprit.core.algorithm.state.UpdateDestinationTimeMiss.TimeMissInfo;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.misc.JobInsertionContext;
import jsprit.core.problem.solution.route.activity.End;
import jsprit.core.problem.solution.route.activity.TourActivity;

public class DestinationSoftTimeConstraint implements SoftActivityConstraint {
    
    private StateManager stateManager;
    private VehicleRoutingProblem vrp;
    private int timeWindowMissCost;

    public DestinationSoftTimeConstraint(StateManager aStateManager, VehicleRoutingProblem aVrp, int aTimeWindowMissCost) {
        stateManager = aStateManager;
        vrp = aVrp;
        timeWindowMissCost = aTimeWindowMissCost;
    }
    
    @Override
    public double getCosts(JobInsertionContext iFacts, TourActivity prevAct, TourActivity newAct, TourActivity nextAct,
            double prevActDepTime) {
        List<TourActivity> subsequentActivities = findSubsequentActivities(iFacts, nextAct);
        double addTime = vrp.getTransportCosts().getTransportTime(prevAct.getLocation(), newAct.getLocation(),
                prevActDepTime, iFacts.getNewDriver(), iFacts.getNewVehicle());
        
        double costs = 0;
        costs += Math.max(0, newAct.getTheoreticalEarliestOperationStartTime() - addTime);  
        costs += Math.max(0, addTime - newAct.getTheoreticalLatestOperationStartTime());
        addTime += newAct.getOperationTime();
        
        for (TourActivity ta : subsequentActivities) {
            costs += activityCost(addTime, ta);
        }
        return costs * timeWindowMissCost;
    }

    private double activityCost(double time, TourActivity ta) {
        double taCost = 0;
        TimeMissInfo missInfo = stateManager.getActivityState(ta, InternalStates.TIME_SLACK, TimeMissInfo.class);
        if (missInfo == null) {
            return 0d;
        }
        double newEndMiss = time + missInfo.getEndMiss();
        double newStartMiss = missInfo.getStartMiss() - time;
        taCost += Math.max(0, newEndMiss);
        taCost += Math.max(0, newStartMiss);
        return taCost;
    }
    
    
    
    private List<TourActivity> findSubsequentActivities(JobInsertionContext iFacts, TourActivity nextAct) {
        List<TourActivity> activities = iFacts.getRoute().getActivities();
        
        if (activities.isEmpty() || nextAct instanceof End) {
            return Collections.emptyList();
        } else {
            return activities.subList(iFacts.getDestinationBaseContext().getInsertionIndex(),
                    activities.size());
        }
        
    }

}

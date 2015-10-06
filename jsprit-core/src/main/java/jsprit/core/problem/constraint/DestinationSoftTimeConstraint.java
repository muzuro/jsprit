package jsprit.core.problem.constraint;

import java.util.Collection;
import java.util.Objects;

import jsprit.core.algorithm.state.InternalStates;
import jsprit.core.algorithm.state.StateManager;
import jsprit.core.algorithm.state.TimeWindowMissUtils;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import jsprit.core.problem.job.Service;
import jsprit.core.problem.misc.JobInsertionContext;
import jsprit.core.problem.solution.route.activity.End;
import jsprit.core.problem.solution.route.activity.Start;
import jsprit.core.problem.solution.route.activity.TourActivity;

public class DestinationSoftTimeConstraint implements SoftActivityConstraint {
    
    private StateManager stateManager;
    private VehicleRoutingProblem vrp;
    private double timeWindowMissCost;
    private double worstTimeMiss;
    private double badTimeMiss;

    /**
     * @param aStateManager
     * @param aVrp
     * @param aTimeWindowMissCostWeight вес непопадания во временные окна
     */
    public DestinationSoftTimeConstraint(StateManager aStateManager, VehicleRoutingProblem aVrp,
            double aTimeWindowMissCostWeight) {
        stateManager = aStateManager;
        vrp = aVrp;
        timeWindowMissCost = aTimeWindowMissCostWeight;
        
        worstTimeMiss = determineMaxCosts(aVrp);
        badTimeMiss = calculateBadTimeMiss(aVrp);
    }
    
    private double calculateBadTimeMiss(VehicleRoutingProblem aVrp) {
        Collection<Service> values = (Collection) vrp.getJobs().values();
        long twJobs = values.stream().filter(s->TimeWindowMissUtils.isDefaultTimeWindow(s.getTimeWindow())).count();
        return twJobs * 2 * 60 * 1000;
    }

    private double determineMaxCosts(VehicleRoutingProblem vrp) {
        double max = 0d;
        Collection<Service> values = (Collection) vrp.getJobs().values();
        for (Service i : values) {
            for (Service j : values) {
                max = Math.max(max, vrp.getTransportCosts().getTransportCost(i.getLocation(), j.getLocation(), 0, null,
                        vrp.getVehicles().iterator().next()));
            }
        }
        return max;
    }
    
    @Override
    public double getCosts(JobInsertionContext iFacts, TourActivity prevAct, TourActivity newAct, TourActivity nextAct,
            double prevActDepTime) {
        VehicleRoutingTransportCosts transportCosts = vrp.getTransportCosts();
        double prevNewTransportTime = transportCosts.getTransportTime(prevAct.getLocation(), newAct.getLocation(),
                prevActDepTime, iFacts.getNewDriver(), iFacts.getNewVehicle());
        double newActDepTime = prevActDepTime + prevNewTransportTime + newAct.getOperationTime();
        double newNextTransportTime = transportCosts.getTransportTime(newAct.getLocation(), nextAct.getLocation(), newActDepTime,
                iFacts.getNewDriver(), iFacts.getNewVehicle());
        
        //cost for this point is calculated in jsprit.core.problem.cost.TimeWindowCosts        
        double addedTime = prevNewTransportTime + newAct.getOperationTime() + newNextTransportTime;
        
        Double pastWaiting;
        Double pastLateness;
        if (newAct instanceof End) {
            pastWaiting = stateManager.getRouteState(iFacts.getRoute(), InternalStates.DestinationBase.PAST_WAITING,
                    Double.class);
            pastLateness = stateManager.getRouteState(iFacts.getRoute(), InternalStates.DestinationBase.PAST_LATENESS,
                    Double.class);
        } else {            
            pastWaiting = stateManager.getActivityState(nextAct, InternalStates.DestinationBase.PAST_WAITING,
                    Double.class);
            pastLateness = stateManager.getActivityState(nextAct, InternalStates.DestinationBase.PAST_LATENESS,
                    Double.class);
        }
        
        Double futureWaiting;
        Double futureLateness;
        if (prevAct instanceof Start) {
            futureWaiting = stateManager.getRouteState(iFacts.getRoute(), InternalStates.DestinationBase.FUTURE_WAITING,
                    Double.class);
            futureLateness = stateManager.getRouteState(iFacts.getRoute(), InternalStates.DestinationBase.FUTURE_LATENESS,
                    Double.class);
        } else {            
            futureWaiting = stateManager.getActivityState(prevAct, InternalStates.DestinationBase.FUTURE_WAITING,
                    Double.class);
            futureLateness = stateManager.getActivityState(prevAct, InternalStates.DestinationBase.FUTURE_LATENESS,
                    Double.class);
        }
        
        if (Objects.isNull(pastWaiting) || Objects.isNull(pastLateness) || Objects.isNull(futureWaiting)
                || Objects.isNull(futureLateness)) {
            return 0;
        }
        
        double usefullPastWaiting = Math.max(pastWaiting, 0);
        double usefullPastLateness = Math.max(pastLateness, 0);
        double usefullFutureWaiting = Math.max(futureWaiting - addedTime, 0);
        double usefullFutureLateness = Math.max(futureLateness + addedTime, 0);
        
        double twCost = usefullPastWaiting + usefullPastLateness + usefullFutureWaiting
                + usefullFutureLateness;
        
//        Double timewindowlessCountWeigth = stateManager.getRouteState(iFacts.getRoute(),
//                InternalStates.DestinationBase.TIMEWINDOWLESS_COUNT_WEIGTH, Double.class);
        
        return normalize(twCost) * timeWindowMissCost;
    }
    
    /**
     * @param aMiss
     * @return
     */
    private double normalize(double aMiss) {
        return (worstTimeMiss * aMiss) / badTimeMiss;
    }
    
}

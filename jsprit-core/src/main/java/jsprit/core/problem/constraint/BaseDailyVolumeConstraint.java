package jsprit.core.problem.constraint;

import java.util.Objects;

import jsprit.core.algorithm.state.DestinationBaseLoadChecker;
import jsprit.core.algorithm.state.InternalStates;
import jsprit.core.problem.Location;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.misc.JobInsertionContext;
import jsprit.core.problem.solution.route.activity.DestinationService;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.solution.route.state.RouteAndActivityStateGetter;

/**
 * Ограничение допустимого объёма точки разгрузки
 */
public class BaseDailyVolumeConstraint implements HardActivityConstraint {

    private RouteAndActivityStateGetter stateManager;

    private DestinationBaseLoadChecker destinationBaseLoadChecker;

    public BaseDailyVolumeConstraint(RouteAndActivityStateGetter stateManager, VehicleRoutingProblem aVrp) {
        super();
        this.stateManager = stateManager;
        destinationBaseLoadChecker = aVrp.getDestinationBaseLoadChecker();
    }

    @Override
    public ConstraintsStatus fulfilled(JobInsertionContext iFacts, TourActivity prevAct, TourActivity newAct,
            TourActivity nextAct, double prevActDepTime) {
        if (!(newAct instanceof DestinationService)) {
            return ConstraintsStatus.FULFILLED;
        }
        
        int runNum = iFacts.getDestinationBaseContext().getRunNum();
        Location runUnloadLocation = stateManager.getRunState(iFacts.getRoute(), runNum,
                InternalStates.DestinationBase.RUN_UNLOAD_LOCATION, Location.class, null);
        //insert allowed if location not loaded
        if (Objects.isNull(runUnloadLocation) || destinationBaseLoadChecker.isLocationDailyLoadable(runUnloadLocation, newAct.getSize())) {
            return ConstraintsStatus.FULFILLED;
        }
        return ConstraintsStatus.NOT_FULFILLED_BREAK;// means not suitible for this run
    }
    
}

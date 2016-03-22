package jsprit.core.algorithm.state;

import java.util.Objects;

import jsprit.core.algorithm.recreate.InsertionData;
import jsprit.core.algorithm.recreate.listener.JobInsertedListener;
import jsprit.core.problem.Location;
import jsprit.core.problem.job.Job;
import jsprit.core.problem.solution.route.VehicleRoute;

public class UpdateUnload implements JobInsertedListener {

    private StateManager stateManager;
    private DestinationBaseLoadChecker destinationBaseLoadChecker;

    public UpdateUnload(StateManager aStateManager, DestinationBaseLoadChecker aDestinationBaseLoadChecker) {
        stateManager = aStateManager;
        destinationBaseLoadChecker = aDestinationBaseLoadChecker;
    }
    
    @Override
    public void informJobInserted(Job aInsertedJob, VehicleRoute aInRoute, InsertionData aIData) {
        Location unloadLocation;
        if (Objects.nonNull(aIData.getBaseToInsert())) {
            unloadLocation = aIData.getBaseToInsert().getLocation();
            stateManager.putRunState(aInRoute, aIData.getInsertionRunNumber(),
                    InternalStates.DestinationBase.RUN_UNLOAD_LOCATION, unloadLocation);
        } else {
            unloadLocation = stateManager.getRunState(aInRoute, aIData.getInsertionRunNumber(),
                    InternalStates.DestinationBase.RUN_UNLOAD_LOCATION, Location.class, null);                        
        }
        destinationBaseLoadChecker.addUnloadVolume(unloadLocation, aInsertedJob.getSize());
    }
    
}

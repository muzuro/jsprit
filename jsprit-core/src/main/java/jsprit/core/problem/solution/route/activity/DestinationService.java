package jsprit.core.problem.solution.route.activity;

import jsprit.core.problem.job.Destination;

public class DestinationService extends PickupService<Destination> {

    public DestinationService(Destination destination) {
        super(destination);
    }
    
    public DestinationService(DestinationService aService) {
        super(aService);
    }
    
    @Override
    public DestinationService duplicate() {
        return new DestinationService(this);
    }
    
    @Override
    public String getName() {
        return getJob().getId();
    }

}

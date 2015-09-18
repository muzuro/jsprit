package jsprit.core.problem.solution.route.activity;

import jsprit.core.problem.job.Base;

public class BaseService extends PickupService<Base> {

    public BaseService(Base aBase) {
        super(aBase);
    }
    
    public BaseService(BaseService aBaseService) {
        super(aBaseService);
    }
    
    @Override
    public BaseService duplicate() {
        return new BaseService(this);
    }

    @Override
    public String getName() {
        return getJob().getId();
    }
    
}

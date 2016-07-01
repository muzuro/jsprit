package jsprit.core.problem.solution.route.activity;

import jsprit.core.problem.job.Base;

public class BaseService extends PickupService<Base> {

    public BaseService(Base aBase) {
        super(aBase);
        setIndex(aBase.getIndex());
    }
    
    public BaseService(BaseService aBaseService) {
        super(aBaseService);
    }
    
    @Override
    public BaseService duplicate() {
        Base job = (Base) getJob();
        return new BaseService(Base.copyOf(job));
    }

    @Override
    public String getName() {
        return getJob().getId();
    }
    
}

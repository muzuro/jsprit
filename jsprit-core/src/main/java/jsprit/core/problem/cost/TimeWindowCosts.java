package jsprit.core.problem.cost;

import jsprit.core.problem.driver.Driver;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.vehicle.Vehicle;

public class TimeWindowCosts implements VehicleRoutingActivityCosts {

    private double timeWindowCostWeight;

    public TimeWindowCosts(double aTimeWindowCostWeight) {
        timeWindowCostWeight = aTimeWindowCostWeight;
    }
    
    @Override
    public double getActivityCost(TourActivity tourAct, double arrivalTime, Driver driver, Vehicle vehicle) {
        double cost = 0d;
        cost += Math.max(0d, tourAct.getTheoreticalEarliestOperationStartTime() - arrivalTime);
        if (vehicle.getEarliestDeparture() < tourAct.getTheoreticalLatestOperationStartTime()) {
            // Не учитываем опоздания если туда невозможно успеть
            cost += Math.max(0d, arrivalTime - tourAct.getTheoreticalLatestOperationStartTime());
        }
        return cost * timeWindowCostWeight;
    }

}

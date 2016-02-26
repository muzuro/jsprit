package jsprit.core.algorithm.recreate;

import java.util.function.Consumer;
import java.util.function.Supplier;

import jsprit.core.algorithm.recreate.DestinationInsertionCalculator.EmptyConsumer;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.vehicle.Vehicle;

/**
 * Created by schroeder on 19/05/15.
 */
class InsertActivity implements Event {

    private VehicleRoute vehicleRoute;

    private Vehicle newVehicle;

    private TourActivity activity;

    private int index;

    private TourActivity baseActivity;

    private Consumer<TourActivity> onInsert;
    
    public InsertActivity(VehicleRoute vehicleRoute, Vehicle newVehicle, TourActivity activity, int index) {
        this.vehicleRoute = vehicleRoute;
        this.newVehicle = newVehicle;
        this.activity = activity;
        this.index = index;
        onInsert = EmptyConsumer.INSTANCE;
    }
    
    public InsertActivity(VehicleRoute vehicleRoute, Vehicle newVehicle, TourActivity activity, int index,
            TourActivity aBaseActivity, Consumer<TourActivity> aOnInsert) {
        this.vehicleRoute = vehicleRoute;
        this.newVehicle = newVehicle;
        this.activity = activity;
        this.index = index;
        baseActivity = aBaseActivity;
        onInsert = aOnInsert;
    }

    public Vehicle getNewVehicle() {
        return newVehicle;
    }

    public VehicleRoute getVehicleRoute() {
        return vehicleRoute;
    }

    public TourActivity getActivity() {
        return activity;
    }

    public int getIndex() {
        return index;
    }

    public TourActivity getBaseActivity() {
        return baseActivity;
    }

    public Consumer<TourActivity> getOnInsert() {
        return onInsert;
    }

    public void setOnInsert(Consumer<TourActivity> aOnInsert) {
        onInsert = aOnInsert;
    }

}

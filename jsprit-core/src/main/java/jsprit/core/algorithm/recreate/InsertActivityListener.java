package jsprit.core.algorithm.recreate;

import java.util.Objects;

/**
 * Created by schroeder on 19/05/15.
 */
class InsertActivityListener implements EventListener {

    @Override
    public void inform(Event event) {
        if (event instanceof InsertActivity) {
            InsertActivity insertActivity = (InsertActivity) event;
            if (!insertActivity.getNewVehicle().isReturnToDepot()) {
                if (insertActivity.getIndex() >= insertActivity.getVehicleRoute().getActivities().size()) {
                    insertActivity.getVehicleRoute().getEnd().setLocation(insertActivity.getActivity().getLocation());
                }
            }
            insertActivity.getVehicleRoute().getTourActivities().addActivity(insertActivity.getIndex(), insertActivity.getActivity());
            if (Objects.nonNull(insertActivity.getBaseActivity())) {
                insertActivity.getVehicleRoute().getTourActivities().addActivity(insertActivity.getIndex() + 1, insertActivity.getBaseActivity());
            }
            ((InsertActivity) event).getOnInsert().accept(insertActivity.getBaseActivity());
        }
    }

}

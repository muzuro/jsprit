package jsprit.core.problem.constraint;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.box.Jsprit;
import jsprit.core.algorithm.box.Jsprit.Parameter;
import jsprit.core.algorithm.state.InternalStates;
import jsprit.core.algorithm.state.StateManager;
import jsprit.core.algorithm.state.UpdateActivityTimes;
import jsprit.core.algorithm.state.UpdateDestinationTimeMiss;
import jsprit.core.problem.Capacity;
import jsprit.core.problem.Location;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.VehicleRoutingProblem.FleetSize;
import jsprit.core.problem.constraint.ConstraintManager.Priority;
import jsprit.core.problem.cost.TimeWindowCosts;
import jsprit.core.problem.job.Base;
import jsprit.core.problem.job.Destination;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.BaseService;
import jsprit.core.problem.solution.route.activity.DestinationService;
import jsprit.core.problem.solution.route.activity.TimeWindow;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.vehicle.VehicleImpl;
import jsprit.core.problem.vehicle.VehicleTypeImpl;
import jsprit.core.util.ActivityTimeTracker;
import jsprit.core.util.Solutions;

public class DestinationBaseTest {

    @Test
    public void testLoadTwoDestinationTwoBase() {
        //services
        Destination d1 = Destination.Builder.newInstance("d1")
            .setLocation(Location.newInstance(50,60))
            .setTimeWindow(new TimeWindow(0, Double.MAX_VALUE))
            .addSizeDimension(0, 10)
            .build();
        Destination d2 = Destination.Builder.newInstance("d2")
            .setLocation(Location.newInstance(20,30))
            .addSizeDimension(0, 10)
            .setTimeWindow(new TimeWindow(0, Double.MAX_VALUE))
            .build();
        Base b1 = Base.Builder.newInstance("b1").setLocation(Location.newInstance(5,5)).build();
        Base b2 = Base.Builder.newInstance("b2").setLocation(Location.newInstance(5,5)).build();
        
        
        // vehicle
        VehicleTypeImpl vt = VehicleTypeImpl.Builder.newInstance("vt").addCapacityDimension(0, 15).build();
        VehicleImpl v1 = VehicleImpl.Builder.newInstance("v1").setType(vt)
                .setStartLocation(Location.newInstance(0,0)).setEarliestStart(0).build();
        
        VehicleRoute initialRoute = VehicleRoute.Builder.newInstance(v1).addService(b1).addService(b2).build();
        
        VehicleRoutingProblem vrp = VehicleRoutingProblem.Builder.newInstance()/*.addJob(b1).addJob(b2)*/.addJob(d1)
                .addJob(d2).addVehicle(v1).setFleetSize(FleetSize.FINITE).addInitialVehicleRoute(initialRoute).build();
        
        StateManager stateManager = new StateManager(vrp);
        stateManager.updateDestainationBaseLoadStates();
        ConstraintManager constraintManager = new ConstraintManager(vrp, stateManager);
        constraintManager.addConstraint(new BaseLoadActivityLevelConstraint(stateManager), Priority.HIGH);
        constraintManager.addConstraint(new DestinationLoadActivityLevelConstraint(stateManager), Priority.HIGH);
        
        VehicleRoutingAlgorithm vra = Jsprit.Builder.newInstance(vrp)
                .addCoreStateAndConstraintStuff(false)
                .setStateAndConstraintManager(stateManager, constraintManager)
                .buildAlgorithm();
        
        vra.setMaxIterations(100);
        VehicleRoutingProblemSolution solution = Solutions.bestOf(vra.searchSolutions());
        VehicleRoute route = solution.getRoutes().iterator().next();
        Iterator<TourActivity> iterator = route.getActivities().iterator();
        Assert.assertTrue(iterator.next() instanceof DestinationService);
        Assert.assertTrue(iterator.next() instanceof BaseService);
        Assert.assertTrue(iterator.next() instanceof DestinationService);
        Assert.assertTrue(iterator.next() instanceof BaseService);
    }
    
    @Test
    public void testLoadManyDestinationTwoBase() {
        //services
        List<Destination> destinations = new ArrayList<Destination>();
        for (int i = 0; i < 23; i++) {
            destinations.add(createDestination(String.format("d%s", i), Location.newInstance(5*i, 10*i), 1));
        }
        Base b1 = Base.Builder.newInstance("b1").setLocation(Location.newInstance(5,5)).build();
        Base b2 = Base.Builder.newInstance("b2").setLocation(Location.newInstance(5,5)).build();
        
        // vehicle
        VehicleTypeImpl vt = VehicleTypeImpl.Builder.newInstance("vt").addCapacityDimension(0, 15).build();
        VehicleImpl v1 = VehicleImpl.Builder.newInstance("v1").setType(vt)
                .setStartLocation(Location.newInstance(0,0)).setEarliestStart(0).build();
        
        VehicleRoute initialRoute = VehicleRoute.Builder.newInstance(v1).addService(b1).addService(b2).build();
        VehicleRoutingProblem vrp = VehicleRoutingProblem.Builder.newInstance().addInitialVehicleRoute(initialRoute)/*.addJob(b1).addJob(b2)*/
                .addAllJobs(destinations).addVehicle(v1).setFleetSize(FleetSize.FINITE).build();
        
        StateManager stateManager = new StateManager(vrp);
        stateManager.updateDestainationBaseLoadStates();
        ConstraintManager constraintManager = new ConstraintManager(vrp, stateManager);
        constraintManager.addConstraint(new BaseLoadActivityLevelConstraint(stateManager), Priority.HIGH);
        constraintManager.addConstraint(new DestinationLoadActivityLevelConstraint(stateManager), Priority.HIGH);
        
        VehicleRoutingAlgorithm vra = Jsprit.Builder.newInstance(vrp)
                .addCoreStateAndConstraintStuff(false)
                .setStateAndConstraintManager(stateManager, constraintManager)
                .buildAlgorithm();
        
        vra.setMaxIterations(10);
        VehicleRoutingProblemSolution solution = Solutions.bestOf(vra.searchSolutions());
        VehicleRoute route = solution.getRoutes().iterator().next();
        Assert.assertEquals(25, route.getActivities().size());
    }
    
    @Test
    public void testLoadTwoBaseOneRun() {
        //services
        List<Destination> destinations = new ArrayList<Destination>();
        for (int i = 0; i < 23; i++) {
            destinations.add(createDestination(String.format("d%s", i), Location.newInstance(5*i, 10*i), 1));
        }
        Base b1 = Base.Builder.newInstance("b1").setLocation(Location.newInstance(5,5)).build();
        Base b2 = Base.Builder.newInstance("b2").setLocation(Location.newInstance(5,5)).build();
        
        // vehicle
        VehicleTypeImpl vt = VehicleTypeImpl.Builder.newInstance("vt").addCapacityDimension(0, 30).build();
        VehicleImpl v1 = VehicleImpl.Builder.newInstance("v1").setType(vt)
                .setStartLocation(Location.newInstance(0,0)).setEarliestStart(0).build();
        
        VehicleRoute initialRoute = VehicleRoute.Builder.newInstance(v1).addService(b1).addService(b2).build();
        VehicleRoutingProblem vrp = VehicleRoutingProblem.Builder.newInstance().addInitialVehicleRoute(initialRoute)
                .addAllJobs(destinations).addVehicle(v1).setFleetSize(FleetSize.FINITE).build();
        
        StateManager stateManager = new StateManager(vrp);
        stateManager.updateDestainationBaseLoadStates();
        ConstraintManager constraintManager = new ConstraintManager(vrp, stateManager);
        constraintManager.addConstraint(new BaseLoadActivityLevelConstraint(stateManager), Priority.HIGH);
        constraintManager.addConstraint(new DestinationLoadActivityLevelConstraint(stateManager), Priority.HIGH);
        
        VehicleRoutingAlgorithm vra = Jsprit.Builder.newInstance(vrp)
                .addCoreStateAndConstraintStuff(false)
                .setStateAndConstraintManager(stateManager, constraintManager)
                .buildAlgorithm();
        
        vra.setMaxIterations(10);
        VehicleRoutingProblemSolution solution = Solutions.bestOf(vra.searchSolutions());
        VehicleRoute route = solution.getRoutes().iterator().next();
        Assert.assertEquals(25, route.getActivities().size());
        System.out.println(route.prettyPrintActivites());
        Assert.assertTrue(route.getActivities().get(23) instanceof BaseService);
        Assert.assertTrue(route.getActivities().get(24) instanceof BaseService);
    }
    
    @Test
    public void testSoftTimeConstraints() {
        //services
        List<Destination> destinations = new ArrayList<Destination>();
        for (int i = 0; i < 10; i++) {
            destinations.add(createDestination(String.format("d%s", i), Location.newInstance(5 * i+1, 10 * i+1), 1));
        }
        destinations.add(createDestination("dtw1", Location.newInstance(52, 10), 10, new TimeWindow(0, 100)));
        destinations.add(createDestination("dtw2", Location.newInstance(54, 18), 10, new TimeWindow(100, 200)));
        destinations.add(createDestination("dtw3", Location.newInstance(56, 14), 10, new TimeWindow(200, 300)));
        Collections.shuffle(destinations);
        
        Base b1 = Base.Builder.newInstance("b1").setLocation(Location.newInstance(5,5)).build();
        
        // vehicle
        VehicleTypeImpl vt = VehicleTypeImpl.Builder.newInstance("vt").addCapacityDimension(0, 100).build();
        VehicleImpl v1 = VehicleImpl.Builder.newInstance("v1").setType(vt).setStartLocation(Location.newInstance(0,0))
                .setEarliestStart(0).build();
        
        VehicleRoute initialRoute = VehicleRoute.Builder.newInstance(v1).addService(b1).build();
        int timeWindowCostWeight = 100;
        VehicleRoutingProblem vrp = VehicleRoutingProblem.Builder.newInstance().addInitialVehicleRoute(initialRoute)
                .addAllJobs(destinations).addVehicle(v1).setFleetSize(FleetSize.FINITE)
                .setActivityCosts(new TimeWindowCosts(timeWindowCostWeight)).build();
        
        StateManager stateManager = new StateManager(vrp);
        stateManager.updateDestainationBaseLoadStates();
        
        UpdateDestinationTimeMiss updateDestinationTimeMiss = new UpdateDestinationTimeMiss(stateManager);
        stateManager.addRuinListener(updateDestinationTimeMiss);
        stateManager.addInsertionListener(updateDestinationTimeMiss);
        stateManager.addStateUpdater(new UpdateActivityTimes(vrp.getTransportCosts(),
                ActivityTimeTracker.ActivityPolicy.AS_SOON_AS_ARRIVED));
        
        ConstraintManager constraintManager = new ConstraintManager(vrp, stateManager);
        constraintManager.addConstraint(new BaseLoadActivityLevelConstraint(stateManager), Priority.HIGH);
        constraintManager.addConstraint(new DestinationLoadActivityLevelConstraint(stateManager), Priority.HIGH);
        constraintManager.addConstraint(new DestinationSoftTimeConstraint(stateManager, vrp, timeWindowCostWeight/*, 200*/));
        
        VehicleRoutingAlgorithm vra = Jsprit.Builder.newInstance(vrp)
                .addCoreStateAndConstraintStuff(false)
                .setStateAndConstraintManager(stateManager, constraintManager)
                .setProperty(Parameter.THRESHOLD_INI, "0.08")
                .setProperty(Parameter.THRESHOLD_ALPHA, "0.3")
//                .setProperty(Strategy.RANDOM_REGRET, "10")
//                .setProperty(Strategy.RANDOM_BEST, "10")
                .buildAlgorithm();
        
        vra.setMaxIterations(500);
        VehicleRoutingProblemSolution solution = Solutions.bestOf(vra.searchSolutions());
        VehicleRoute route = solution.getRoutes().iterator().next();
        System.out.println(String.format("%s-%s", solution.getCost(), route.prettyPrintActivitesWithTimes()));
        
        int tw1Ind = -1;
        int tw2Ind = -1;
        int tw3Ind = -1;
        int i = 0;
        List<TourActivity> activities = route.getActivities();
        for (TourActivity ta : activities) {
            if (ta.getName().equals("dtw1")) {
                tw1Ind = i;
            } else if (ta.getName().equals("dtw2")) {
                tw2Ind = i;
            } else if (ta.getName().equals("dtw3")) {
                tw3Ind = i;
            }
            i++;
        }
        Assert.assertTrue(tw1Ind < tw2Ind);
        Assert.assertTrue(tw2Ind < tw3Ind);
    }

    private Destination createDestination(String id, Location newInstance, int capacity) {
        return Destination.Builder.newInstance(id)
            .setLocation(newInstance)
            .setTimeWindow(new TimeWindow(0, Double.MAX_VALUE))
            .addSizeDimension(0, capacity)
            .build();
    }
    
    private Destination createDestination(String id, Location newInstance, int capacity, TimeWindow aTimeWindow) {
        return Destination.Builder.newInstance(id)
            .setLocation(newInstance)
            .setTimeWindow(aTimeWindow)
            .addSizeDimension(0, capacity)
            .build();
    }
    
    @Test
    public void testRunState() {
        StateManager stateManager = new StateManager(Mockito.mock(VehicleRoutingProblem.class));
        VehicleRoute route = Mockito.mock(VehicleRoute.class);
        Mockito.when(route.isEmpty()).thenReturn(false);
        
        Capacity fistCapacity = Capacity.Builder.newInstance().addDimension(0, 15).build();
        stateManager.putRunState(route, 0, InternalStates.DestinationBase.RUN_LOAD, fistCapacity);
        Capacity fromState = stateManager.getRunState(route, 0, InternalStates.DestinationBase.RUN_LOAD, Capacity.class, null);
        Assert.assertEquals(fistCapacity, fromState);
        
        Capacity secondCapacity = Capacity.Builder.newInstance().addDimension(0, 90).build();
        Capacity oldVal = stateManager.putRunState(route, 0, InternalStates.DestinationBase.RUN_LOAD, secondCapacity);
        Assert.assertEquals(fistCapacity, oldVal);
        fromState = stateManager.getRunState(route, 0, InternalStates.DestinationBase.RUN_LOAD, Capacity.class, null);
        Assert.assertEquals(secondCapacity, fromState);
    }
    
}

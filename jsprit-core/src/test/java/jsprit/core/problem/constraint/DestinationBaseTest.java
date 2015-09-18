package jsprit.core.problem.constraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.box.Jsprit;
import jsprit.core.algorithm.state.InternalStates;
import jsprit.core.algorithm.state.StateManager;
import jsprit.core.problem.Capacity;
import jsprit.core.problem.Location;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.VehicleRoutingProblem.FleetSize;
import jsprit.core.problem.constraint.ConstraintManager.Priority;
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
import jsprit.core.util.Solutions;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

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
        
        VehicleRoutingProblem vrp = VehicleRoutingProblem.Builder.newInstance().addJob(b1).addJob(b2).addJob(d1)
                .addJob(d2).addVehicle(v1).setFleetSize(FleetSize.FINITE).build();
        
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
        Collections.shuffle(destinations);
        Base b1 = Base.Builder.newInstance("b1").setLocation(Location.newInstance(5,5)).build();
        Base b2 = Base.Builder.newInstance("b2").setLocation(Location.newInstance(5,5)).build();
        
        // vehicle
        VehicleTypeImpl vt = VehicleTypeImpl.Builder.newInstance("vt").addCapacityDimension(0, 15).build();
        VehicleImpl v1 = VehicleImpl.Builder.newInstance("v1").setType(vt)
                .setStartLocation(Location.newInstance(0,0)).setEarliestStart(0).build();
        
        VehicleRoutingProblem vrp = VehicleRoutingProblem.Builder.newInstance().addJob(b1).addJob(b2)
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
        
        vra.setMaxIterations(1000);
        VehicleRoutingProblemSolution solution = Solutions.bestOf(vra.searchSolutions());
        VehicleRoute route = solution.getRoutes().iterator().next();
        Assert.assertEquals(25, route.getActivities().size());
        System.out.println(route.prettyPrintActivites());
        Assert.assertTrue(route.getActivities().get(15) instanceof BaseService);
        Assert.assertTrue(route.getActivities().get(24) instanceof BaseService);
    }
    
    @Test
    public void testTwoBaseOneRun() {
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
        
        VehicleRoutingProblem vrp = VehicleRoutingProblem.Builder.newInstance().addJob(b1).addJob(b2)
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
        Assert.assertTrue(route.getActivities().get(15) instanceof BaseService);
        Assert.assertTrue(route.getActivities().get(24) instanceof BaseService);
    }

    private Destination createDestination(String id, Location newInstance, int capacity) {
        return Destination.Builder.newInstance(id)
            .setLocation(newInstance)
            .setTimeWindow(new TimeWindow(0, Double.MAX_VALUE))
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

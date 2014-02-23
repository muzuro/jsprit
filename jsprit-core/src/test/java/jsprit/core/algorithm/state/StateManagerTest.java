package jsprit.core.algorithm.state;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import jsprit.core.problem.Capacity;
import jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.solution.route.state.StateFactory;
import jsprit.core.problem.solution.route.state.StateFactory.State;
import jsprit.core.problem.solution.route.state.StateFactory.StateId;

import org.junit.Test;

public class StateManagerTest {
	
	@SuppressWarnings("deprecation")
	@Test
	public void whenInternalRouteStateIsSet_itMustBeSetCorrectly(){
		VehicleRoute route = mock(VehicleRoute.class);
		StateManager stateManager = new StateManager(mock(VehicleRoutingTransportCosts.class));
		StateId id = StateFactory.createId("myState");
		State state = StateFactory.createState(1.);
		stateManager.putInternalRouteState(route, id, state);
		assertEquals(1.,stateManager.getRouteState(route, id).toDouble(),0.01);
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void whenRouteStateIsSet_itMustBeSetCorrectly(){
		VehicleRoute route = mock(VehicleRoute.class);
		StateManager stateManager = new StateManager(mock(VehicleRoutingTransportCosts.class));
		StateId id = StateFactory.createId("myState");
		State state = StateFactory.createState(1.);
		stateManager.putRouteState(route, id, state);
		assertEquals(1.,stateManager.getRouteState(route, id).toDouble(),0.01);
	}
	
	@Test
	public void whenRouteStateIsSetWithGenericMethod_itMustBeSetCorrectly(){
		VehicleRoute route = mock(VehicleRoute.class);
		StateManager stateManager = new StateManager(mock(VehicleRoutingTransportCosts.class));
		StateId id = StateFactory.createId("myState");
		State state = StateFactory.createState(1.);
		stateManager.putRouteState_(route, id, State.class, state);
		assertEquals(1.,stateManager.getRouteState(route, id, State.class).toDouble(),0.01);
	}
	
	@Test
	public void whenRouteStateIsSetWithGenericMethodAndBoolean_itMustBeSetCorrectly(){
		VehicleRoute route = mock(VehicleRoute.class);
		StateManager stateManager = new StateManager(mock(VehicleRoutingTransportCosts.class));
		StateId id = StateFactory.createId("myState");
		boolean routeIsRed = true;
		stateManager.putRouteState_(route, id, Boolean.class, routeIsRed);
		assertTrue(stateManager.getRouteState(route, id, Boolean.class));
	}
	
	@Test
	public void whenRouteStateIsSetWithGenericMethodAndInteger_itMustBeSetCorrectly(){
		VehicleRoute route = mock(VehicleRoute.class);
		StateManager stateManager = new StateManager(mock(VehicleRoutingTransportCosts.class));
		StateId id = StateFactory.createId("myState");
		int load = 3;
		stateManager.putRouteState_(route, id, Integer.class, load);
		int getLoad = stateManager.getRouteState(route, id, Integer.class);
		assertEquals(3, getLoad);
	}
	
	@Test
	public void whenRouteStateIsSetWithGenericMethodAndCapacity_itMustBeSetCorrectly(){
		VehicleRoute route = mock(VehicleRoute.class);
		StateManager stateManager = new StateManager(mock(VehicleRoutingTransportCosts.class));
		StateId id = StateFactory.createId("myState");
		Capacity capacity = Capacity.Builder.newInstance().addDimension(0, 500).build();
		stateManager.putRouteState_(route, id, Capacity.class, capacity);
		Capacity getCap = stateManager.getRouteState(route, id, Capacity.class);
		assertEquals(500, getCap.get(0));
	}
	
	

	@SuppressWarnings("deprecation")
	@Test
	public void whenInternalActivityStateIsSet_itMustBeSetCorrectly(){
		TourActivity activity = mock(TourActivity.class);
		StateManager stateManager = new StateManager(mock(VehicleRoutingTransportCosts.class));
		StateId id = StateFactory.createId("myState");
		State state = StateFactory.createState(1.);
		stateManager.putInternalActivityState(activity, id, state);
		assertEquals(1.,stateManager.getActivityState(activity, id).toDouble(),0.01);
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void whenActivityStateIsSet_itMustBeSetCorrectly(){
		TourActivity activity = mock(TourActivity.class);
		StateManager stateManager = new StateManager(mock(VehicleRoutingTransportCosts.class));
		StateId id = StateFactory.createId("myState");
		State state = StateFactory.createState(1.);
		stateManager.putActivityState(activity, id, state);
		assertEquals(1.,stateManager.getActivityState(activity, id).toDouble(),0.01);
	}
	
	@Test
	public void whenActivityStateIsSetWithGenericMethod_itMustBeSetCorrectly(){
		TourActivity activity = mock(TourActivity.class);
		StateManager stateManager = new StateManager(mock(VehicleRoutingTransportCosts.class));
		StateId id = StateFactory.createId("myState");
		State state = StateFactory.createState(1.);
		stateManager.putActivityState_(activity, id, State.class, state);
		assertEquals(1.,stateManager.getActivityState(activity, id, State.class).toDouble(),0.01);
	}
	
	@Test
	public void whenActivityStateIsSetWithGenericMethodAndBoolean_itMustBeSetCorrectly(){
		TourActivity activity = mock(TourActivity.class);
		StateManager stateManager = new StateManager(mock(VehicleRoutingTransportCosts.class));
		StateId id = StateFactory.createId("myState");
		boolean routeIsRed = true;
		stateManager.putActivityState_(activity, id, Boolean.class, routeIsRed);
		assertTrue(stateManager.getActivityState(activity, id, Boolean.class));
	}
	
	@Test
	public void whenActivityStateIsSetWithGenericMethodAndInteger_itMustBeSetCorrectly(){
		TourActivity activity = mock(TourActivity.class);
		StateManager stateManager = new StateManager(mock(VehicleRoutingTransportCosts.class));
		StateId id = StateFactory.createId("myState");
		int load = 3;
		stateManager.putActivityState_(activity, id, Integer.class, load);
		int getLoad = stateManager.getActivityState(activity, id, Integer.class);
		assertEquals(3, getLoad);
	}
	
	@Test
	public void whenActivityStateIsSetWithGenericMethodAndCapacity_itMustBeSetCorrectly(){
		TourActivity activity = mock(TourActivity.class);
		StateManager stateManager = new StateManager(mock(VehicleRoutingTransportCosts.class));
		StateId id = StateFactory.createId("myState");
		Capacity capacity = Capacity.Builder.newInstance().addDimension(0, 500).build();
		stateManager.putActivityState_(activity, id, Capacity.class, capacity);
		Capacity getCap = stateManager.getActivityState(activity, id, Capacity.class);
		assertEquals(500, getCap.get(0));
	}
}

package eu.opends.car;

import eu.opends.drivingTask.scenario.ScenarioLoader;
import eu.opends.drivingTask.scenario.ScenarioLoader.CarProperty;
import eu.opends.main.SimulationDefaults;
import eu.opends.main.Simulator;

/**
 * @version 1 (2016-03-31)
 * 
 * @author Christian-Nils Boda (Chalmers University)
 */
public class Brake {
	
	private Car car;
	private static float decelerationBrake;
	private static float decelerationFreeWheel;
	private static float maxFreeWheelBrakeForce;
	private static float maxBrakeForce;
	private static float maxBrakeForcePerWheel;
	private static float maxFreeWheelBrakeForcePerWheel;
	private static float carMass;
	
	public Brake(Car car){
		// Assign the input car to the class
		this.car = car;
		// Get the vehicle mass
		carMass = car.getMass();
		
		// load settings from driving task file
		ScenarioLoader scenarioLoader = Simulator.getDrivingTask().getScenarioLoader();	
		// Maximum Engine braking 
		decelerationFreeWheel = scenarioLoader.getCarProperty(CarProperty.brake_decelerationFreeWheel, 
				SimulationDefaults.brake_decelerationFreeWheel);
		// Maximum global brakes deceleration
		decelerationBrake = scenarioLoader.getCarProperty(CarProperty.brake_decelerationBrake, 
				SimulationDefaults.brake_decelerationBrake);
		
		// Maximum brake force due to the engine in free wheel (no gas pedal intensity)
		maxFreeWheelBrakeForce = decelerationFreeWheel * carMass;
		// Get the maximum brake force due to the engine per wheel //FIXME: should be per driven wheels (i.e. 2 or 4)
		maxFreeWheelBrakeForcePerWheel = maxFreeWheelBrakeForce / 2f;
		// Maximum brake force due to the brakes
		maxBrakeForce = decelerationBrake * carMass;
		// Get the maximum brake force per wheel //FIXME: If brake balance differs from 50/50 (front/rear)
		maxBrakeForcePerWheel = maxBrakeForce / 4f;
		
	}
	
	public float getBrakingForcePerWheel() {
		// Calculate the applied brake force which is linearly proportional to the maxBrakeForce
		return car.getBrakePedalIntensity() * maxBrakeForcePerWheel;				
	}
	public float getFrictionForcePerWheel() {
		// Calculate the force due to the engine brake proportional to the current friction coefficient //FIXME: the force should be only applied to the driven wheels
		return car.getPowerTrain().getFrictionCoefficient() * maxFreeWheelBrakeForcePerWheel; 
	}
	
	public float getMaxBrakingForcePerWheel(){
		return maxBrakeForcePerWheel;
	}
	public float getMaxBrakingForce(){
		return maxBrakeForce;
	}
	
	
}

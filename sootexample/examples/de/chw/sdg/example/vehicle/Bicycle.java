package de.chw.sdg.example.vehicle;

public class Bicycle extends Vehicle {

	@Override
	protected void drive() {
		System.out.println("Driving 20 km/h");
		bicycleFast();
		bicycleFast();
		bicycleSlow();
	}
	
	private void bicycleFast(){
		
	}
	
	public void bicycleSlow(){
		
	}

}


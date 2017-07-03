package de.chw.sdg.example.vehicle;

import java.util.Random;

public class Car extends Vehicle {

	@Override
	protected void drive() {
		int round = 0;
		int count = 5;
		getInCar();
		for (int i = round; i <= 3; i++) {
			while (count > 0) {
				driveFaster();
				count--;
			}
		}
		Random ran = new Random();
		int position = ran.nextInt(3) + 1;
		getOutOfCar();
		if (position == 1) {
			winningPose();
			System.out.println("Yeah!");
		} else {
			loosingPose();
		}
	}

	private void loosingPose() {
		// TODO Auto-generated method stub

	}

	private void winningPose() {
		// TODO Auto-generated method stub

	}

	private void getOutOfCar() {
		// TODO Auto-generated method stub

	}

	private void driveFaster() {
		// TODO Auto-generated method stub

	}

	private void getInCar() {
		// TODO Auto-generated method stub

	}

}

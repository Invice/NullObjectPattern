package de.jek.examples.presentation.usedObjects;

import java.util.HashMap;
import java.util.Map;

public class Stock {

	private Map<Long, Integer> availability = new HashMap<Long, Integer>();

	public void setAvailabilityFor(long productid, int availability) {
		this.availability.put(productid, availability);
	}

}

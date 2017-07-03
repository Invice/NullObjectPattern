package de.jek.examples.presentation.usedObjects;

import de.jek.examples.successiveMethods.usedObjects.ConnectionException;
import de.jek.examples.successiveMethods.usedObjects.IDataServerConnection;

public class Customer {

	private String name;
	private long id;
	private int noPurchases = 0;
	private String postcode;

	public Customer(long id, String name, int noPurchases, String postcode) {
		this.name = name;
		this.id = id;
		this.noPurchases = noPurchases;
		this.postcode = postcode;
	}

	public void incrementNoPurchases(IDataServerConnection dataSC)
			throws ConnectionException {
		noPurchases = +1;
		dataSC.incrementNoPurchasesOfCustomer(id);
	}

	public String getPostcode() {
		return postcode;
	}

	public int getNoPurchases() {
		return noPurchases;
	}

	public String getName() {
		return name;
	}

}

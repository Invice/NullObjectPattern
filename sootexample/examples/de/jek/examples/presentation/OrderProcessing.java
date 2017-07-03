package de.jek.examples.presentation;

import de.jek.examples.presentation.usedObjects.Customer;
import de.jek.examples.presentation.usedObjects.Reporting;
import de.jek.examples.successiveMethods.usedObjects.ConnectionException;
import de.jek.examples.successiveMethods.usedObjects.IDataServerConnection;

public class OrderProcessing {

	public static void dependentSuccessiveMethodCalls(
			IDataServerConnection dataSC, long customerid, int productid)
			throws ConnectionException {

		Customer customer = dataSC.retrieveCustomer(customerid);

		customer.incrementNoPurchases(dataSC);

		Reporting.addRelations(dataSC, customer, productid);
	}
}

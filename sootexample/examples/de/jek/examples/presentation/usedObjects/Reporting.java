package de.jek.examples.presentation.usedObjects;

import de.jek.examples.successiveMethods.usedObjects.ConnectionException;
import de.jek.examples.successiveMethods.usedObjects.IDataServerConnection;

public class Reporting {

	public static void addRelations(IDataServerConnection dataSC,
			Customer customer, int productid) throws ConnectionException {

		dataSC.updatePostcodeNoPurchasesRelation(customer.getPostcode(),
				customer.getNoPurchases());
		dataSC.addPostcodeProductIDRelation(customer.getPostcode(), productid);
	}

}

package de.jek.examples.presentation;

import java.util.logging.Logger;

import de.jek.examples.successiveMethods.usedObjects.ConnectionException;
import de.jek.examples.successiveMethods.usedObjects.IDataServerConnection;
import de.jek.examples.successiveMethods.usedObjects.IEventServerConnection;

public class DatabaseUpdate {

	private static Logger LOGGER = Logger.getLogger("DatabaseUpdate");

	public static void independentSuccessiveMethodCalls(
			IDataServerConnection dataSC, IEventServerConnection eventSC)
			throws ConnectionException {

		dataSC.updateProducts(eventSC);
		LOGGER.info("Products are successfully updated: outdated products were deleted, new ones inserted.");
		dataSC.updateProductPrices(eventSC);
		LOGGER.info("Product Prices are successfully updated.");
		dataSC.updateAvailability(eventSC);
		LOGGER.info("Stock is successfully updated.");
		int numNewCustomers = dataSC.addNewCustomers(eventSC);
		LOGGER.info(numNewCustomers
				+ "New customers are successfully inserted in the database.");

		LOGGER.info("Database is up-to-date.");

	}

}

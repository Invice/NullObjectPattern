package de.jek.examples.presentation;

import java.util.logging.Logger;

import de.jek.examples.successiveMethods.usedObjects.ConnectionException;
import de.jek.examples.successiveMethods.usedObjects.IDataServerConnection;
import de.jek.examples.successiveMethods.usedObjects.IEventServerConnection;

public class ConnectionSetup {

	private static Logger LOGGER = Logger.getLogger("ConnectionSetup");

	public static void independentDirectlySuccessiveMethodCalls(
			IDataServerConnection dataSC, IEventServerConnection eventSC)
			throws ConnectionException {

		dataSC.connect(100000);
		eventSC.connect();

		LOGGER.info("Connection successfully established.");
	}

}

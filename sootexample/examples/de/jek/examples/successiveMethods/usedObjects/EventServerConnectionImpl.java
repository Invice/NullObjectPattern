package de.jek.examples.successiveMethods.usedObjects;

import java.util.logging.Logger;

public class EventServerConnectionImpl implements IEventServerConnection {

	private static Logger LOGGER = Logger.getLogger("EventServerConnection");

	/**
	 * Dummy implementation which just spends time.
	 */
	@Override
	public void connect() {
		try {
			LOGGER.info("Trying to connect to EventServer...");
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// do nothing
		}
		LOGGER.info("Connection to EventServer successfully established...");
	}

}

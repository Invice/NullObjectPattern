package de.jek.examples.successiveMethods;

import de.jek.examples.successiveMethods.usedObjects.ConnectionException;
import de.jek.examples.successiveMethods.usedObjects.DataServerConnectionImpl;
import de.jek.examples.successiveMethods.usedObjects.EventServerConnectionImpl;
import de.jek.examples.successiveMethods.usedObjects.IDataServerConnection;
import de.jek.examples.successiveMethods.usedObjects.IEventServerConnection;

public class ConnectionSetup {

	public static void main(String[] args) throws ConnectionException {
		IDataServerConnection dataSC = new DataServerConnectionImpl();
		IEventServerConnection eventSC = new EventServerConnectionImpl();

		String hostname = dataSC.connect(100000);
		eventSC.connect();

		System.out.println("Hello " + hostname);
	}

}

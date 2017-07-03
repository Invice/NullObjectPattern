package de.jek.examples.presentation;

import java.util.logging.Logger;

import de.jek.examples.successiveMethods.usedObjects.ConnectionException;
import de.jek.examples.successiveMethods.usedObjects.DataServerConnectionImpl;
import de.jek.examples.successiveMethods.usedObjects.EventServerConnectionImpl;
import de.jek.examples.successiveMethods.usedObjects.IDataServerConnection;
import de.jek.examples.successiveMethods.usedObjects.IEventServerConnection;

public class ClassWithSuccessiveMethodCalls {

	private static Logger LOGGER = Logger
			.getLogger("ClassWithSuccessiveMethodCalls");

	public static void main(String[] args) throws ConnectionException {

		LOGGER.info("Application started.");

		IDataServerConnection dataSC = new DataServerConnectionImpl();
		IEventServerConnection eventSC = new EventServerConnectionImpl();

		ConnectionSetup.independentDirectlySuccessiveMethodCalls(dataSC,
				eventSC);

		DatabaseUpdate.independentSuccessiveMethodCalls(dataSC, eventSC);

		String input = javax.swing.JOptionPane
				.showInputDialog("Enter the product id of the item you want to purchase:");
		int productid = Integer.parseInt(input);

		input = javax.swing.JOptionPane
				.showInputDialog("Enter your customer id:");
		long customerid = Long.parseLong(input);

		OrderProcessing.dependentSuccessiveMethodCalls(dataSC, customerid,
				productid);

		LOGGER.info("Application terminated.");

	}

}

package de.jek.examples.successiveMethods;

import java.util.concurrent.Callable;

import de.jek.examples.ParallelisationUtil;
import de.jek.examples.successiveMethods.usedObjects.DataServerConnectionImpl;
import de.jek.examples.successiveMethods.usedObjects.EventServerConnectionImpl;
import de.jek.examples.successiveMethods.usedObjects.IDataServerConnection;
import de.jek.examples.successiveMethods.usedObjects.IEventServerConnection;

public class ConnectionSetupParallelised_Thesis {

	public static void main(String[] args) {

		IDataServerConnection dataSC = new DataServerConnectionImpl();
		IEventServerConnection eventSC = new EventServerConnectionImpl();

		int nProcessors = ParallelisationUtil.NUMBER_OF_PROCESSORS;
		java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors
				.newFixedThreadPool(nProcessors);

		DataSCConnectCallable callable1 = new ConnectionSetupParallelised_Thesis.DataSCConnectCallable(
				dataSC);
		java.util.concurrent.Future<?> future1 = pool.submit(callable1);

		EventSCConnectCallable callable2 = new ConnectionSetupParallelised_Thesis.EventSCConnectCallable(
				eventSC);
		java.util.concurrent.Future<?> future2 = pool.submit(callable2);

		try {
			future1.get();
			future2.get();
		} catch (InterruptedException e) {
		} catch (java.util.concurrent.ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof Error) {
				throw (Error) cause;
			}
			if (cause instanceof RuntimeException) {
				throw (RuntimeException) cause;
			}
		}
		pool.shutdown();

		System.out.println("Hello world");

	}

	private static class DataSCConnectCallable implements Callable<Void> {

		private IDataServerConnection dataSC;

		public DataSCConnectCallable(IDataServerConnection dataSC) {
			super();
			this.dataSC = dataSC;
		}

		@Override
		public Void call() throws Exception {
			dataSC.connect();
			return null;
		}
	}

	private static class EventSCConnectCallable implements Callable<Void> {

		private IEventServerConnection eventSC;

		public EventSCConnectCallable(IEventServerConnection eventSC) {
			super();
			this.eventSC = eventSC;
		}

		@Override
		public Void call() {
			eventSC.connect();
			return null;
		}
	}

}

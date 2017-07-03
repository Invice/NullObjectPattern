package de.jek.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ParallelisedClassWithLoop {

	public static void main(String[] args) {
		int noProcessors = ParallelisationUtil.NUMBER_OF_PROCESSORS;
		ExecutorService pool = Executors.newFixedThreadPool(noProcessors);

		List<Future<Void>> listOfFutures = new ArrayList<Future<Void>>();
		for (int i = 0; i < 10; i++) {
			LoopCallable call = new LoopCallable(i);
			java.util.concurrent.Future<Void> future = pool.submit(call);
			listOfFutures.add(future);
		}
		try {
			for (Future<Void> f : listOfFutures) {
				f.get();
			}
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
	}

	private static void writeResultInDatabase(double result) {
		try {
			Thread.sleep(600);
		} catch (InterruptedException e) {
		}
	}

	private static double calculateSomethingForQuiteAWhile(int i) {
		// long calculation
		try {
			Thread.sleep(400);
		} catch (InterruptedException e) {
		}
		return 1234.567 * i;
	}

	static class LoopCallable implements Callable<Void> {

		int i;

		public LoopCallable(int i) {
			this.i = i;
		}

		@Override
		public Void call() throws Exception {
			double result = calculateSomethingForQuiteAWhile(i);
			writeResultInDatabase(result);
			return null;
		}

	}

}

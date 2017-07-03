package de.jek.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ParallelisedClassWithLoopOnArray {

	public static void main(String[] args) {

		int[] array = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };

		int nThreads = ParallelisationUtil.NUMBER_OF_PROCESSORS;
		ExecutorService pool = Executors.newFixedThreadPool(nThreads);

		int entriesPerThread = array.length / nThreads;
		int remainedEntries = array.length % nThreads;
		int startindex = 0;
		int endindex = 0;
		List<Future<Integer>> futureResults = new ArrayList<Future<Integer>>();
		for (int i = 0; i < nThreads; i++) {
			endindex = startindex + entriesPerThread - 1;
			if (remainedEntries > 0) {
				endindex++;
				remainedEntries--;
			}
			SumCallable callable = new SumCallable(array, startindex, endindex);
			Future<Integer> futureSum = pool.submit(callable);
			futureResults.add(futureSum);
			startindex = endindex + 1;
		}

		int sum = 0;

		try {
			for (Future<Integer> f : futureResults) {
				sum = sum + f.get();
			}
		} catch (InterruptedException e) {
		} catch (ExecutionException e) {
		}

		System.out.println("Sum = " + sum);
	}

	private static class SumCallable implements Callable<Integer> {

		int startindex;
		int endindex;
		int[] array;

		public SumCallable(int[] array, int startindex, int endindex) {
			this.array = array;
			this.startindex = startindex;
			this.endindex = endindex;
		}

		@Override
		public Integer call() throws Exception {
			int sum = 0;
			for (int i = startindex; i <= endindex; i++) {
				sum = sum + array[i];
			}
			return sum;
		}

	}
}

package de.johl.examples;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class ParallelStaticFuncReturn {	
	private static int a = 1;
	private static int b = 1;
	
	public static Integer writeA1()	{
		a *= 2;
		return a;
	}	
	
	public static int readB() {
		return b * 2;		
	}
	
	private static class Task implements Callable<Integer>	{
		public Integer call() throws Exception {
			return writeA1();
		}
	}
	
	public static void main(String[] args) {	
		Task task = new Task();	
		ForkJoinPool pool = new ForkJoinPool();
		ForkJoinTask<Integer> submission = pool.submit(task);
		
		int j = readB();		
		int i = submission.join();
		
		doSomething(i,j);
	}
	
	private static void doSomething(int a, int b) {}
}

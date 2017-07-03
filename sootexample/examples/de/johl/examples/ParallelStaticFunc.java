package de.johl.examples;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class ParallelStaticFunc {
	@SuppressWarnings("unused")
	private static int a = 1;
	private static int b = 1;
	
	public static void writeA1()
	{
		a *= 2;		
	}	
	
	public static int readB()
	{
		return b * 2;		
	}		
	
	private static class Task0 implements Runnable
	{
		public void run()
		{
			writeA1();
		}
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {	
		
		Task0 task0 = new Task0();	
		ForkJoinPool pool = new ForkJoinPool();
		ForkJoinTask<?> result0 = pool.submit(task0);
		
		int i3 = readB();		
		result0.join();				
	}	
}

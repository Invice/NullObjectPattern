package de.johl.examples;

import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class ParallelStaticFuncReturn2 {
	
	private static int a = 1;
	private static int b = 1;
	
	public static Integer writeA1()
	{
		a *= 2;
		return a;
	}	
	
	public static int readB()
	{
		return b * 2;		
	}

	public static void main(String[] args) {	
		
		ForkJoinPool pool = new ForkJoinPool();
		ForkJoinTask<Integer> result0 = pool.submit(new Callable<Integer>() {
			@Override
			public Integer call() throws Exception 
			{
				return writeA1();
			}			
		});
		
		int i3 = readB();		
		int i1 = result0.join();
		
		System.out.println("ret: " + (i1 + i3));
	}	
}

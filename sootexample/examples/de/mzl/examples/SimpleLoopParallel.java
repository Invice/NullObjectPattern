package de.mzl.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class SimpleLoopParallel {

	public static void main(String[] args) {
		ForkJoinPool pool = new ForkJoinPool();
		List<ForkJoinTask<?>> taskList = new ArrayList<ForkJoinTask<?>>();
		
		for(int i=0; i < 10; i++){
			final int  i2 = i;
			ForkJoinTask<?> task = pool.submit(new Runnable() {
				@Override
				public void run() {
					System.out.println(i2);
				}				
			});
			
			taskList.add(task);
		}
		
		for(ForkJoinTask<?> t : taskList) {
			t.join();
		}
	}

}

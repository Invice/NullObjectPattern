package de.johl.examples;

public class ParallelMethod {
	public static void main(String[] args) {

		Data dataA = new Data();
		Data dataB = new Data();
		Foo f1 = new Foo(dataA);
		Foo f2 = new Foo(dataB);
		Foo f3 = new Foo(dataB);
		
		//f1,f2 in parallel is ok
		//f2,f3 in parallel is bad, because of shared dataB
		f1.update();
		f2.update();
		f3.update();
	}
	
	private static class Data
	{
		public int i = 0;
	}
	
	private static class Foo
	{
		public Data data;
		
		Foo(Data d)
		{
			data = d;
		}
		
		void update()
		{
			if(data != null)
			{
				if(data.i % 2 == 0)
					data.i += 1;
				else
					data.i *= 2;
			}
		}
	}
}

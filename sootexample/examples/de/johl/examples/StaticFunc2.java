package de.johl.examples;

public class StaticFunc2 {
	
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
	
	public static void foo()
	{
		for(int i=0; i<10; ++i)
		{			
			writeA1();
			a = a - 10;
		}
		
		if(a > 100)
		{
			a = 0;
		}
		else
		{
			a = 1000;
		}
	}
	
	@SuppressWarnings("unused")
	public static void main(String[] args) {
	
		writeA1();
		int i1 = readB();
		foo();
		int i3 = readB();
		System.out.println("down");
	}	
}

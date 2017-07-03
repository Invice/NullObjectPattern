package de.johl.examples;

public class StaticFunc {
	
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

	@SuppressWarnings("unused")
	public static void main(String[] args) {
	
		writeA1();
		int i3 = readB();
	}	
}

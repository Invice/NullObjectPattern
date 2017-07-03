package de.johl.examples;

public class StaticFuncReturn  {	
	private static int a = 1;
	private static int b = 1;
	
	public static int writeA1()	{
		a *= 2;
		return a;
	}
	
	public static int readB() {
		return b * 2;		
	}		

	public static void main(String[] args) {
		int i = writeA1();
		int j = readB();	
		
		doSomething(i,j);
	}	
	
	private static void doSomething(int a, int b) {
		StaticFuncReturn.a = a;
		StaticFuncReturn.b = b;
	}
}

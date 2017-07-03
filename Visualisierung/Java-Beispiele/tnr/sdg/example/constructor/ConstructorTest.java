package de.tnr.sdg.example.constructor;

public class ConstructorTest {
	
	private TestObject inline = new TestObject();
	private TestObject inConstructor;
	private TestObject inMethod;
	private TestObject returnValue;
	private TestObject nullObject = null;
	private TestObject noInit;
	
	public ConstructorTest() {
		inConstructor = new TestObject();
		foo();
		returnValue = foo3();
	}
	
	public void foo() {
		foo2();
	}
	
	private void foo2() {
		inMethod = new TestObject();
	}
	
	public TestObject foo3() {
		TestObject tmp = new TestObject();
		return tmp;
	}
	
	public static void main(String[] args) {
		ConstructorTest test = new ConstructorTest();
	}
}

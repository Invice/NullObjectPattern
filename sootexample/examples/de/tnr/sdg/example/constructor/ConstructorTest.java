package de.tnr.sdg.example.constructor;

public class ConstructorTest {
	
	private TestObject inline = new TestObject();
	private TestObject inConstructor;
	private TestObject inMethod;
	private TestObject returnValue;
	private TestObject nullObject = null;
	private TestObject noInit;
	private final TestObject finalObject;
	
	public ConstructorTest() {
		inConstructor = new TestObject();
		foo();
		returnValue = foo3();
		
		//Meldung: Match gefunden, ist final. If-Anweisung kann gelöscht werden.
		finalObject = new TestObject();
	}

	//If inline == null -> NullObject
	public void setInline(TestObject inline) {
		this.inline = inline;
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
	
	/*
	 * TODO: Checker.java mit eigener main-Methode analysieren/evaluierten.
	 * -> Update query
	 * -> Gliederung BA
	 */
	
	
	public static void main(String[] args) {
		ConstructorTest test = new ConstructorTest();
	}
}

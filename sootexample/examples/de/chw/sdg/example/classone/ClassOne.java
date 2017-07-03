package de.chw.sdg.example.classone;

public class ClassOne {

	public ClassTwo class2 = new ClassTwo();
	
	public static void main(String[] args) {
		ClassOne class1 = new ClassOne();
		class1.class2.foo4();
	}
}

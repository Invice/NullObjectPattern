package de.chw.sdg.example.classone;

public class ClassTwo {

	public ClassTwo() {
		foo1();
		try {
			foo2(1);
		} catch (Exception e) {
			System.out.println("error");
		}
		ClassThree clazz = new ClassThree();
		clazz.foo3();
	}

	public ClassTwo(String string) {
		System.out.println(string);
	}

	private void foo1() {
		// TODO Auto-generated method stub

	}

	private void foo2(int i) {
		// TODO Auto-generated method stub

	}
}

package de.chw.sdg.example.generics;

import java.util.ArrayList;
import java.util.List;

public class GenericClass<A, B extends Object, C extends List<?>> {

	private A a;

	@SuppressWarnings("unused")
	public static void main(final String[] args) {
		List<Integer> intList0 = new ArrayList<Integer>();
		// Soot does not support the diamond operator
//		List<Integer> intList1 = new ArrayList<>();

		GenericClass<Long, String, List<?>> genericClass = new GenericClass<Long, String, List<?>>();
		Long a2 = genericClass.a;
	}
}

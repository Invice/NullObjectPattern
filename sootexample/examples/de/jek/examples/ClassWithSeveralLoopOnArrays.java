package de.jek.examples;

public class ClassWithSeveralLoopOnArrays {

	public static void main(String[] args) {

		int[] array = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

		sumArray(array);
		sumArray2(array);

		multiplyArray(array);

		concatStrings();

	}

	private static void concatStrings() {
		String[] array = { "Hello", " ", "darling" };
		String result = ""; // better StringBuilder..
		for (int i = 0; i < array.length; i++) {
			result = result + array[i];
		}
		System.out.println(result);
	}

	private static void sumArray(int[] array) {
		int sum = 0;
		for (int i = 0; i < array.length; i++) {
			sum = sum + array[i];
		}
		System.out.println("Sum = " + sum);
	}

	private static void sumArray2(int[] array) {
		int sum = 0;
		for (int i = 0; i < array.length; i++) {
			sum = +array[i];
		}
		System.out.println("Sum = " + sum);
	}

	private static void multiplyArray(int[] array) {
		int product = 1;
		for (int i = 0; i < array.length; i++) {
			product = product * array[i];
		}
		System.out.println("Product = " + product);
	}
}

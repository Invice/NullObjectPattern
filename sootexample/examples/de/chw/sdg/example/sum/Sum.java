package de.chw.sdg.example.sum;

public class Sum {

	public static void main(String[] args) {
		Sum sum = new Sum();
		int result = sum.sum(new int[] { 1, 2, 3 });
		System.out.println("result: " + result);
	}

	public int sum(final int[] a) {
		int sum = 0;
		for (int i = 0; i < a.length; i++) {
			sum = sum + a[i];
		}
		return sum;
	}

	public void eko_sum() {
		int[] array = {1,2,3,4,5,6,7,8,9,10};
		int sum = 0;
		for (int i=0; i < array.length; i++) {
			sum = sum + array[i];
		}
		System.out.println("Sum = " + sum);
	}

	@SuppressWarnings("unused")
	private void emptyMethod() {
		// Soot produces at least two units: "this" assignment and "return" statement
	};
}

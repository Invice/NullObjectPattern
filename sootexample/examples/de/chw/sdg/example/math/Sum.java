package de.chw.sdg.example.math;

public class Sum {

	public int sum(final int[] a) {
		int sum = 0;
		for (int i = 0; i < a.length; i++) {
			// sum = sum + a[i];
			add(sum + a[i]);
		}
		return sum;
	}

	private void add(int i) {
		System.out.println("i: " + i);
	}

	public static void main(String[] args) {
		new Sum().sum(new int[] { 4, 3, 2, 1 });
	}
}

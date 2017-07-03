package de.chw.sdg.example.sum;

public class GlobalSum {

	private final int[] numbers;

	public GlobalSum(int[] a) {
		this.numbers = a;
	}

	public static void main(String[] args) {
		GlobalSum sum = new GlobalSum(new int[] { 1, 2, 3 });
		int result = sum.sum();
		System.out.println("result: " + result);
	}

	public int sum() {
		int sum = 0;
		for (int i = 0; i < numbers.length; i++) {
			sum = sum + numbers[i];
		}
		return sum;
	}
}

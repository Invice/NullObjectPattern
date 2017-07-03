package de.chw.sdg.example.branch;

public class Switch {

	private static int x;

	public static void main(String[] args) {
		int a;
		switch (x) {
		case 0: {
			a = 0;
			break;
		}
		case 1: {
			a = 1;
			break;
		}
		default: {
			a = x;
			break;
		}
		}

		System.out.println("a=" + a);
	}
}

package de.chw.sdg.example.branch;

import java.io.IOException;
import java.nio.charset.IllegalCharsetNameException;

public class If implements Cloneable {

	private static int x;

	public static void main(final String[] args) throws IOException, IllegalCharsetNameException {
		int readValue = x + 11;
		if (readValue < 2) {
			x = 20;
		} else {
			x = 0;
		}
		long timestamp = System.nanoTime();
		System.out.println(1 + timestamp);
	}

	public static void subIf() {
		if (x < 2) {
			if (x < 4) {
				x = 20;
			} else {
				x = 10;
			}
		} else {
			x = 0;
		}
	}

	public int twoReturns() {
		if (x < 2) {
			return (x + 1) * 2; // Soot optimizes (1+3) to 4
		} else {
			return 2124;
		}
	}

	// public int twoReturnsNoElse() {
	// if (x < 2) {
	// return x + 1;
	// }
	// // let's Soot fail
	// return 2124;
	// }
}

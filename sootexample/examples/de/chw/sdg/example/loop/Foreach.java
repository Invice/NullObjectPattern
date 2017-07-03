package de.chw.sdg.example.loop;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Foreach {

	public static void main(final String[] args) {
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			System.out.println(arg);
		}

		List<String> argsAsList = toIterable(args);
		for (String arg : argsAsList) {
			System.out.println(arg);
		}

		for (String arg : toIterable(args)) {
			System.out.println(arg);
		}

		for (String arg : Collections.synchronizedCollection(toIterable(args))) {
			System.out.println(arg);
		}

		for (String arg : args) {
			System.out.println(arg);
		}

		for (String arg : getArgs(args)) {
			System.out.println(arg);
		}

		for (String arg : getArgs(args).clone()) {
			System.out.println(arg);
		}

	}

	private static List<String> toIterable(final String[] args) {
		return Arrays.asList(args);
	}

	private static String[] getArgs(final String[] args) {
		return args;
	}
}

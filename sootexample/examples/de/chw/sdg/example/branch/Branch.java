package de.chw.sdg.example.branch;

public class Branch {

	public static void main(String[] args) {
		Object obj = new Object();
		obj.hashCode();

		if (args.length > 0) {
			System.out.println("args[0]: " + args[0]);
		}

		if (Runtime.getRuntime().availableProcessors() > 1) { // isbranchtarget:true
			System.out.println("multi-core system");
		} else {
			System.out.println("single-core system"); // isbranchtarget:true
		}

		for (int i = 0; i < args.length; i++) {
			System.out.println("arg #" + i + ": " + args[i]);
		}

		for (String arg : args) {
			System.out.println("arg: " + arg);
		}

		return; // isbranchtarget:true
	}

}

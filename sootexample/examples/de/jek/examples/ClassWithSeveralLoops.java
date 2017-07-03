package de.jek.examples;

import java.util.ArrayList;
import java.util.List;

public class ClassWithSeveralLoops {

	public static void main(String[] args) {

		List<ImportantObject> list = new ArrayList<ImportantObject>();
		list.add(new ImportantObject(1));
		list.add(new ImportantObject(2));
		list.add(new ImportantObject(3));
		list.add(new ImportantObject(4));

		loopInDep(list);

		loopNext(list);

		loopDep(list, 7);

		loopJustRead(list, 12, new ImportantObject(42));
	}

	private static void loopDep(List<ImportantObject> list, double x) {
		for (ImportantObject o : list) {
			double result = x * o.getId();
			x = result;
			x = 2.2;
			x = result;
		}

	}

	private static void loopDep(List<ImportantObject> list, int x) {
		for (ImportantObject o : list) {
			int result = x;
			x = 3;
			x = 2;
			x = result;
		}

	}

	private static void loopJustRead(List<ImportantObject> list, int x,
			ImportantObject before) {
		for (ImportantObject o : list) {
			double result = x * o.getId();
			o.setResult(result);
			// writeResultInDatabase(result);
		}
	}

	private static void loopInDep(List<ImportantObject> list) {
		for (ImportantObject o : list) {
			double result = calculateSomethingForQuiteAWhile(o);
			writeResultInDatabase(result);
		}

	}

	private static void loopNext(List<ImportantObject> list) {
		for (ImportantObject o : list) {
			double result = calculateSomethingForQuiteAWhile(o);
			o.setResult(result);
		}
	}

	private static void writeResultInDatabase(double result) {
		try {
			Thread.sleep(600);
		} catch (InterruptedException e) {
		}
	}

	private static double calculateSomethingForQuiteAWhile(ImportantObject o) {
		// long calculation
		try {
			Thread.sleep(400);
		} catch (InterruptedException e) {
		}
		return 1234.567 * o.getId();
	}

	private static class ImportantObject {
		int id;
		double result;

		public ImportantObject(int id) {
			this.id = id;
		}

		public void setResult(double result) {
			this.result = result;
		}

		public int getId() {
			return id;
		}
	}

}

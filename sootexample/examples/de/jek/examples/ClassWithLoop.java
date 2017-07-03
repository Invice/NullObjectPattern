package de.jek.examples;

import java.util.ArrayList;
import java.util.List;

public class ClassWithLoop {

	public static void main(String[] args) {

		List<ImportantObject> list = new ArrayList<ImportantObject>();
		list.add(new ImportantObject(1));
		list.add(new ImportantObject(2));
		list.add(new ImportantObject(3));
		list.add(new ImportantObject(4));
		for (ImportantObject o : list) {
			double result = calculateSomethingForQuiteAWhile(o);
			o.setResult(result);
			// writeResultInDatabase(result);
		}

	}

	// private static void writeResultInDatabase(double result) {
	// try {
	// Thread.sleep(600);
	// } catch (InterruptedException e) {
	// }
	// }

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

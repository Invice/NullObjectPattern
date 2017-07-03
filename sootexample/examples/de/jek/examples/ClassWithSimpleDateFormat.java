package de.jek.examples;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ClassWithSimpleDateFormat {

	public static void main(String[] args) throws ParseException {

		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
		String dateString1 = "01.01.2014";
		String dateString2 = "31.03.2015";

		Date date1 = sdf.parse(dateString1);
		Date date2 = sdf.parse(dateString2);
		long difference = date2.getTime() - date1.getTime();
		long days = TimeUnit.DAYS.convert(difference, TimeUnit.MILLISECONDS);
		// if (days > 0) {
		// System.out.println(dateString1 + " ist " + days + " Tage vor "
		// + dateString2);
		// } else {
		// System.out.println(dateString1 + " ist " + days * (-1)
		// + " Tage nach " + dateString2);
		// }
		System.out.println(days
				+ " Tage Unterschied zwischen den beiden Daten.");
	}
}

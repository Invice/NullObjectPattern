package de.chw.sdg.example.loop;

import java.io.File;
import java.util.List;

public class IpdpsForeach {

	public static void main(String[] args) {
		IpdpsForeach main = new IpdpsForeach();
		List<File> files = null;
		main.process(files);
	}

	private boolean debugEnabled;

	public void process(List<File> files) {
		int x = 0;
		for (File file : files) {
			if (debugEnabled) {
				System.out.println("Processing" + file);
			}
			x = x + 1;
			// process file
		}
	}
}

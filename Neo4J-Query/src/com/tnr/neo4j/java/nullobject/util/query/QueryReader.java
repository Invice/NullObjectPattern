package com.tnr.neo4j.java.nullobject.util.query;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class QueryReader {
	
	public static String readQuery(String relativePath) {
		
		String query = "";
		String filePath = new File("").getAbsolutePath() 
				+ System.getProperty("file.separator");
		
		try {			
			BufferedReader reader = new BufferedReader(
					new FileReader(filePath + relativePath));
			String line = reader.readLine();
			if (line != null) {
				query = line;
			}
			
			while ((line = reader.readLine()) != null) {
				query = query.concat(" \n" + line);
			}
			
			reader.close();
			
		} catch (IOException ex) {
			System.err.println("Could not find match query on " + 
					filePath + relativePath);
			ex.printStackTrace();
		}
		
		return query;
	}	
}

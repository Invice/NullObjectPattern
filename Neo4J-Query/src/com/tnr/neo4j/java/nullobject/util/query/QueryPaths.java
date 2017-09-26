package com.tnr.neo4j.java.nullobject.util.query;

public class QueryPaths {
	
	public static String MATCH = "src" + dash() +
			"com" + dash() +
			"tnr" + dash() + 
			"neo4j" + dash() + 
			"java" + dash() +
			"nullobject" + dash() +
			"matching" + dash() +
			"MATCH-Query.txt";
	
	
	private static String dash(){
		return System.getProperty("file.separator");
	}
}

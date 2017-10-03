package com.tnr.neo4j.java.nullobject.util.query;

public class QueryPaths {
	
	
	private static String rawMatchQueryPath = "src/com/tnr/neo4j/java/nullobject/matching/matchQuery.txt";
	private static String rawUninitializedFieldQueryPath = "src/com/tnr/neo4j/java/nullobject/transformation/uninitializedFieldQuery.txt";
	private static String rawMethodQueryPath = "src/com/tnr/neo4j/java/nullobject/transformation/methodQuery.txt";
	private static String rawNameQueryPath = "src/com/tnr/neo4j/java/nullobject/nameQuery.txt";
	
	public static String matchQueryPath = rawMatchQueryPath.replace("/", System.getProperty("file.separator"));	
	public static String uninitializedFieldQueryPath = rawUninitializedFieldQueryPath.replace("/", System.getProperty("file.separator"));
	public static String methodQueryPath = rawMethodQueryPath.replace("/", System.getProperty("file.separator"));
	public static String nameQueryPath = rawNameQueryPath.replace("/", System.getProperty("file.separator"));	
}

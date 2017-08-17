package com.tnr.neo4j.java.nullobject;

public class Main {
	
	public static void main(String[] args) {
		Transformation test = new Transformation();
		test.createIndexes();
//		test.getIndexes();
		test.match();
		test.transform();
	}
}
